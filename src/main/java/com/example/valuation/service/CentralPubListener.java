package com.example.valuation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.valuation.dto.CanonicalTradeDTO;
import com.example.valuation.entity.ValuationEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.sushmithashiva04ops.centraleventpublisher.listener.DynamicOutboxListener;
import com.example.valuation.service.DlqService;;
@Service
public class CentralPubListener {

    private static final Logger logger = LoggerFactory.getLogger(CentralPubListener.class);

    private static final int QUEUE_CAPACITY = 1000;
    private static final int MAX_BATCH_SIZE = 50;

    private final DynamicOutboxListener outboxListener;



    @Autowired
    private ValuationService valuationService;

    @Autowired
    private StatusTrackingService statusTrackingService;

    @Autowired
    private ObjectMapper objectMapper;

     @Autowired
    private DlqService dlqService;

    private final BlockingQueue<String> bufferQueue =
            new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    private int lastFetchedSize = 0;

    public CentralPubListener(DynamicOutboxListener outboxListener) {
        this.outboxListener = outboxListener;
    }

    @Scheduled(fixedRate = 60000)
    public void fetchAndBufferMessages() {

        int currentSize = outboxListener.getQueueSize("valid.mq");

        if (lastFetchedSize >= currentSize) {
            return;
        }

        List<String> allMessages = outboxListener.getMessages("valid.mq");
        List<String> newMessages =
                allMessages.subList(lastFetchedSize, currentSize);

        for (String msg : newMessages) {
            try {
                bufferQueue.put(msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        lastFetchedSize = currentSize;
        logger.info("Buffered {} messages", newMessages.size());
    }

    @Scheduled(fixedRate = 5000)
    public void processBatch() {

        int queueSize = bufferQueue.size();
        if (queueSize == 0) {
            return;
        }

        int dynamicBatchSize = queueSize / 10;

        if (dynamicBatchSize < 1) {
            dynamicBatchSize = 1;
        }

        if (dynamicBatchSize > MAX_BATCH_SIZE) {
            dynamicBatchSize = MAX_BATCH_SIZE;
        }

        List<String> rawBatch = new ArrayList<>(dynamicBatchSize);
        bufferQueue.drainTo(rawBatch, dynamicBatchSize);

        if (rawBatch.isEmpty()) {
            return;
        }

        List<CanonicalTradeDTO> trades = new ArrayList<>();

        for (String msg : rawBatch) {
            try {
                trades.add(objectMapper.readValue(msg, CanonicalTradeDTO.class));
            } catch (Exception e) {
                sendToDLQ(msg, "Deserialization failed: " + e.getMessage());
            }
        }

        if (trades.isEmpty()) {
            return;
        }

        try {

            List<ValuationEntity> results = valuationService.valuationBatch(trades);

            for (ValuationEntity valuation : results) {
                try {
                    statusTrackingService.trackStatus(valuation, null);
                } catch (Exception statusEx) {
                    sendToDLQ(valuation, "Status tracking failed: " + statusEx.getMessage());
                }
            }

        } catch (Exception batchException) {

            for (CanonicalTradeDTO trade : trades) {
                try {
                    ValuationEntity valuation =
                            valuationService.valuation(trade);

                    try {
                        statusTrackingService.trackStatus(valuation, null);
                    } catch (Exception statusEx) {
                        sendToDLQ(valuation, "Status tracking failed: " + statusEx.getMessage());
                    }

                } catch (Exception singleEx) {

                    try {
                        statusTrackingService.trackStatus(trade, singleEx);
                    } catch (Exception statusEx) {
                       sendToDLQ(trade, "Status tracking failed: " + statusEx.getMessage());
                    }
                    sendToDLQ(trade, singleEx.getMessage());
                    
                }
            }
        }
    }


    private void sendToDLQ(Object payload, String reason) {
        dlqService.saveToDlq(payload, reason);
    }

}
