package com.example.valuation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exception_outbox")
public class ExceptionOutboxEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "error_code", length = 20, nullable = false)
    private String errorCode;

    @Column(name = "error_reason", columnDefinition = "TEXT", nullable = false)
    private String errorReason;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "status", length = 20)
    private String status = "PENDING";
}
