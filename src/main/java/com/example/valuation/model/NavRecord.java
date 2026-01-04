package com.example.valuation.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NavRecord(
                BigDecimal navValue,
                String navTime,
                LocalDate navDate) {
}
