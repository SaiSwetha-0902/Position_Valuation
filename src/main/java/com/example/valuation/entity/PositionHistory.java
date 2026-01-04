package com.example.valuation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "position_history", indexes = {
        @Index(name = "idx_distributor_fund_snapshot_date", columnList = "distributor_id,fund_id,snapshot_date"),
        @Index(name = "idx_snapshot_date", columnList = "snapshot_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long distributorId;

    @Column(nullable = false)
    private Long fundId;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal navValue;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal totalValue;

    @Column(nullable = false)
    private LocalDate snapshotDate; // ✅ DATE, not datetime

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // ✅ immutable
}
