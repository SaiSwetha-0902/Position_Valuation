package com.example.valuation.service;

import com.example.valuation.dto.CanonicalTradeDTO;
import com.example.valuation.entity.ValuationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StatusTrackingService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${app.redis.stream:status-stream}")
    private String streamKey;

    public void trackStatus(Object entity, Exception e) {
        Map<String, String> payload = new HashMap<>();

        String status;
        String orderId = "unknown";
        String distributorId = "unknown";
        String fileId = null;

        if (entity instanceof ValuationEntity val) {
            if (val.getFileId() != null) {
                fileId = val.getFileId().toString();
            }
            distributorId = val.getFirmNumber().toString();
            orderId = val.getRawOrderId().toString();
            
            if (e != null) {
                status = "VALUATION_FAILED";
            } else if ("REJECT".equalsIgnoreCase(val.getConfirmedStatus())) {
                status = "REJECTED_TRADE";
            } else if ("CONFIRMED".equalsIgnoreCase(val.getConfirmedStatus())) {
                status = "VALUATED";
            } else {
                status = "NOT_VALUATED";
            }
        } else if (entity instanceof CanonicalTradeDTO trade) {
            if (trade.getFileId() != null) {
                fileId = trade.getFileId().toString();
            }
            distributorId = trade.getFirmNumber().toString();
            orderId = trade.getRawOrderId().toString();
            status = "VALUATION_FAILED";
        } else {
            status = "UNKNOWN";
        }

        payload.put("fileid", fileId);
        payload.put("distributorId", distributorId);
        payload.put("orderId", orderId);
        payload.put("sourceservice", "valuation-service");
        payload.put("status", status);

        StringBuilder payloadJson = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            if (!first) payloadJson.append(",");
            payloadJson.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        payloadJson.append("}");

        Map<String, String> streamData = new HashMap<>();
        streamData.put("payload", payloadJson.toString());

        redisTemplate.opsForStream().add(
            StreamRecords.newRecord()
                .in(streamKey)
                .ofMap(streamData)
        );
    }
}


