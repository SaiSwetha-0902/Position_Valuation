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

@Service
public class CentralPubListener {

    private static final Logger logger = LoggerFactory.getLogger(CentralPubListener.class);

    private static final int QUEUE_CAPACITY = 1000;
    private static final int BATCH_SIZE = 50;

    private final DynamicOutboxListener outboxListener;

    @Autowired
    private ValuationService valuationService;
    
    @Autowired
    private StatusTrackingService statusTrackingService;
   

    @Autowired
    private ObjectMapper objectMapper;

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

    List<String> rawBatch = new ArrayList<>(BATCH_SIZE);
    bufferQueue.drainTo(rawBatch, BATCH_SIZE);

    if (rawBatch.isEmpty()) return;

    List<CanonicalTradeDTO> trades = new ArrayList<>();

    for (String msg : rawBatch) {
        try {
            trades.add(objectMapper.readValue(msg, CanonicalTradeDTO.class));
        } catch (Exception e) {
            sendToDLQ(msg, e.getMessage());
        }
    }

    if (trades.isEmpty()) return;

    try {

        List<ValuationEntity> results = valuationService.valuationBatch(trades);

        for (ValuationEntity valuation : results) {
            try 
            {
                statusTrackingService.trackStatus(valuation, null);
            } catch (Exception statusEx) {
                handleStatusFailure(valuation, statusEx);
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
                    handleStatusFailure(valuation, statusEx);
                }

            } catch (Exception singleEx) {

            	/*
                ValuationEntity failedValuation =
                        valuationService.buildRejectedOrPartial(trade);

                try {
                    statusTrackingService.trackStatus(failedValuation, singleEx);
                } catch (Exception statusEx) {
                    handleStatusFailure(failedValuation, statusEx);
                }

                sendToDLQ(trade, singleEx.getMessage());
                */
            }
        }
    }
}
    private void handleStatusFailure(ValuationEntity valuation, Exception statusEx) {
        sendToDLQ(valuation, "Status tracking failed: " + statusEx.getMessage());
    }


    private void sendToDLQ(Object payload, String reason) {
        logger.error("DLQ payload={}, reason={}", payload, reason);
    }
}
