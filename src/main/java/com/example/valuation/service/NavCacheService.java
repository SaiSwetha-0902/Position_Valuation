package com.example.valuation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.example.valuation.dto.NavRecordDTO;
import java.util.Map;
import com.example.valuation.model.*;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.example.valuation.dto.NavRecordDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NavCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    public NavCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public NavRecordDTO getNavByFundId(Long fundId) {

        String key = fundId.toString(); // Redis key = fundId
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key);

        if (values == null || values.isEmpty()) {
            throw new RuntimeException("NAV not found for fundId: " + fundId);
        }

        NavRecordDTO dto = new NavRecordDTO();
        dto.setDate(values.get("date").toString());
        dto.setTime(values.get("time").toString());
        dto.setNav(Double.parseDouble(values.get("nav").toString()));

        return dto;
    }
}
