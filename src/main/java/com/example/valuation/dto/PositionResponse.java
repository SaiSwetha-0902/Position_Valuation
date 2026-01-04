package com.example.valuation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionResponse {
    private Long distributorId;
    private Long fundId;
    private BigDecimal quantity;
    private BigDecimal navValue;
    private BigDecimal totalValue;
    private LocalDateTime lastUpdated;
}
