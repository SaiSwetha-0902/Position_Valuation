package com.example.valuation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.valuation.dao.InboxDao;
import com.example.valuation.dto.CanonicalTradeDTO;
import com.example.valuation.entity.Inbox;
import com.example.valuation.entity.InboxStatus;
import com.example.valuation.entity.ValuationEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.sushmithashiva04ops.centraleventpublisher.listener.DynamicOutboxListener;
import com.example.valuation.service.DlqService;;
@Service
public class CentralPubListener {

    private static final Logger logger = LoggerFactory.getLogger(CentralPubListener.class);

    @Autowired
    private ValuationService valuationService;

    @Autowired
    private StatusTrackingService statusTrackingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DlqService dlqService;
     
    @Autowired
    private InboxDao inboxDao;

    // For extracting ID from payload received via MQ
    private UUID extractEventId(String payload) {
         try {
         	logger.info("Fetching ID from payload..");
             JsonNode root = objectMapper.readTree(payload);
             return UUID.fromString(root.get("id").asText());
         } catch (Exception e) {
         	logger.error("Fetching ID from payload failed!");
             throw new IllegalArgumentException("Invalid payload: missing ID", e);
         }
     }
     
     // Listens to publisher MQ for live messages and stores in inbox
     @JmsListener(destination = "valid.mq")
     @Transactional
     public void onMessage(String message) {
     	UUID eventId = extractEventId(message); // from payload/header

     	/*
     	 * No point in checking duplicates because eventId is @Id 
     	 * in entity so naturally unique constraints
     	 */
     	/*
         if (inboxDao.existsByEventId(eventId)) {
             return; // duplicate delivery, safe to ignore
         }
         */

     	logger.info("Fetched ID from payload: {}. Creating inbox entry..", eventId);
         Inbox inboxEntry = new Inbox();
         inboxEntry.setEventId(eventId);
         inboxEntry.setPayload(message);
         inboxEntry.setStatus(InboxStatus.NEW);
         inboxEntry.setCreatedAt(LocalDateTime.now());
         
         inboxDao.save(inboxEntry);
         logger.info("Inbox entry created successfully for payload with ID: {}", eventId);
     }
     
     // Runs every 5 seconds to process messages in inbox
     @Scheduled(fixedRate = 5000)
     @Transactional
     public void processMessages() {

         // 1. Atomically claim NEW inbox rows 
         List<Inbox> inboxBatch = inboxDao.claimBatch(100);

         if (inboxBatch.isEmpty()) {
             return;
         }

         List<CanonicalTradeDTO> trades = new ArrayList<>();

         // 2. Deserialize payloads
         for (Inbox inbox : inboxBatch) {
             try {
                 CanonicalTradeDTO trade =
                         objectMapper.readValue(inbox.getPayload(), CanonicalTradeDTO.class);

                 trades.add(trade);

             } catch (Exception e) {
                 inboxDao.markFailed(inbox.getEventId());
                 sendToDLQ(inbox.getPayload(),
                         "Deserialization failed: " + e.getMessage());
             }
         }

         if (trades.isEmpty()) {
             return;
         }

         // 3. Batch valuation
         List<ValuationEntity> results =
                 valuationService.valuationBatch(trades);

         // 4. Post-processing per valuation
         for (ValuationEntity valuation : results) {
             UUID eventId = valuation.getId();

             try {
                 statusTrackingService.trackStatus(valuation, null);
                 inboxDao.markDone(eventId);

             } catch (Exception statusEx) {
                 inboxDao.markFailed(eventId);
                 sendToDLQ(valuation,
                         "Status tracking failed: " + statusEx.getMessage());
             }
         }
     }

    private void sendToDLQ(Object payload, String reason) {
        dlqService.saveToDlq(payload, reason);
    }

}
