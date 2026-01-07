package com.example.valuation.entity;

public enum InboxStatus {
    NEW, // Payload from producer is received and stored in DB
    //PROCESSING, // Payload is fetched by service for processing
    DONE, // Payload data consumed by business logic successfully
    FAILED // Payload couldn't be processed
}
