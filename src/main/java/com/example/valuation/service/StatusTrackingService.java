package com.example.valuation.service;

import com.example.valuation.entity.ValuationEntity;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.StreamRecords;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.beans.factory.annotation.Value;  

@Service
public class StatusTrackingService {

    @Autowired  
    private StringRedisTemplate redisTemplate; 

    @Value("${app.redis.stream:status-stream}")
    private String streamKey;


    public void trackStatus(ValuationEntity val, Exception e) 
    {
        Map<String, String> payload = new HashMap<>();
        
        if(val.getFileId()!=null) {
            payload.put("fileid", val.getFileId().toString());
        }else {
        	payload.put("fileid", null);
        }

        payload.put("distributorId", val.getFirmNumber().toString());
        payload.put("orderId", val.getRawOrderId().toString());
        payload.put("sourceservice", "valuation-service");
      
        String status;

        if ("REJECT".equalsIgnoreCase(val.getConfirmedStatus())) {
            status = "REJECTED_TRADE";

        } else if ("CONFIRMED".equalsIgnoreCase(val.getConfirmedStatus())
                && e == null) {
            status = "VALUATED";

        } else {
            status = "NOT_VALUATED";
        }

        payload.put("status", status);
            
            String payloadJson = "{";
            boolean first = true;
            for (Map.Entry<String, String> entry : payload.entrySet()) {
                if (!first) payloadJson += ",";
                payloadJson += "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"";
                first = false;
            }
            payloadJson += "}";

            Map<String, String> streamData = new HashMap<>();
            streamData.put("payload", payloadJson);

            RecordId recordId = redisTemplate.opsForStream().add(
                StreamRecords.newRecord()
                    .in(streamKey)
                    .ofMap(streamData)
            ); 
    }


}
