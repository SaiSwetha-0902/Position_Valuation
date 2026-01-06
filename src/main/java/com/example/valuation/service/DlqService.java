package com.example.valuation.service;

import com.example.valuation.dto.CanonicalTradeDTO;
import com.example.valuation.entity.ValuationEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import jakarta.jms.Queue;
import java.util.HashMap;
import java.util.Map;

@Service
public class DlqService {
    
    private static final Logger logger = LoggerFactory.getLogger(DlqService.class);
    private static final String DLQ_NAME = "valuation.dlq";
    
    @Autowired
    private JmsTemplate jmsTemplate;
    
    @Autowired
    private Queue dlqQueue;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public void saveToDlq(Object payload, String reason) {
        try {
            Map<String, Object> dlqMessage = new HashMap<>();
            dlqMessage.put("payload", objectMapper.writeValueAsString(payload));
            dlqMessage.put("reason", reason);
            dlqMessage.put("timestamp", System.currentTimeMillis());
            dlqMessage.put("payloadType", payload.getClass().getSimpleName());

            String tradeId = extractTradeId(payload);
            dlqMessage.put("tradeId", tradeId);
            
            jmsTemplate.convertAndSend(dlqQueue, dlqMessage);
            logger.warn("Sent to DLQ '{}': tradeId={}, reason={}", DLQ_NAME, tradeId, reason);
            
        } catch (Exception e) {
            logger.error("Failed to send to DLQ: payload={}, reason={}, error={}", 
                        payload, reason, e.getMessage());
        }
    }
    
    private String extractTradeId(Object payload) {
        if (payload instanceof ValuationEntity) {
            return ((ValuationEntity) payload).getId().toString();
        } else if (payload instanceof CanonicalTradeDTO) {
            return ((CanonicalTradeDTO) payload).getId().toString();
        }
        return "unknown";
    }
}
