package com.example.valuation.config;
import jakarta.jms.Queue;

import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

@Configuration
public class JmsConfig {

    @Bean
    public Queue dlqQueue() {
        return new ActiveMQQueue("DLQ.valuation");
    }
}
