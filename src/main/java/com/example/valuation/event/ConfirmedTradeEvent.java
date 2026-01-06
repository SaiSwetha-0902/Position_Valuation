package com.example.valuation.event;

import java.util.UUID;

public class ConfirmedTradeEvent {

    private final UUID valuationId;

    public ConfirmedTradeEvent(UUID valuationId) {
        this.valuationId = valuationId;
    }

    public UUID getValuationId() {
        return valuationId;
    }
}