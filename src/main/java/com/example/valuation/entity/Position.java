package com.example.valuation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "position")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @EmbeddedId
    private PositionId id;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal navValue;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal totalValue;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @Version
    private Long version;
}
