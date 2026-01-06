package com.example.valuation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;



@Entity
@Table(name = "valuation", 
    indexes = {
        @Index(name = "idx_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_trade_datetime", columnList = "trade_datetime"),
        @Index(name = "idx_client_account", columnList = "client_account_no")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_raw_order_transaction", columnNames = {"raw_order_id", "transaction_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValuationEntity {
    @Id
    private UUID id;
   
    @NotNull
    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @NotNull
    @Column(name = "originator_type")
    private Integer originatorType;
    
    @NotNull
    @Column(name = "firm_number")
    private Integer firmNumber;
    
    @NotNull
    @Column(name = "fund_number")
    private Integer fundNumber;
    
    @NotBlank
    @Column(name = "transaction_type")
    private String transactionType;
    
    @NotBlank
    @Column(name = "transaction_id", unique = true)
    private String transactionId;
    
    @Column(name = "raw_order_id")
    @NotNull
    private UUID rawOrderId; 
    
    @Column(name = "file_id")
    private UUID fileId; 
    
    @Column(name = "order_source")
    @NotBlank
    private String orderSource;  
    
    @Column(name = "trade_datetime")
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime tradeDateTime;
    
    @Column(name = "client_account_no")
    @NotNull
    private Integer clientAccountNo;
    
    @Column(name = "client_name")
    @NotBlank
    private String clientName;
    
    @Column(name = "ssn")
    @NotBlank
    private String ssn;
    
    @Column(name = "dob")
    @NotNull
    private LocalDate dob;
    
    @Column(name = "share_quantity")
    private BigDecimal shareQuantity;


    @Column(name = "valuation_amount")
    @NotNull
    private BigDecimal valuationAmount;

    @Column(name = "valuation_date")
    @NotNull
    private LocalDate valuationDate;

    @Column(name = "caluclatedBy")
    @NotBlank
    private String caluclatedBy;

    @Column(name="confirmed_status", length=20)
    @NotBlank
    private String confirmedStatus;

    @Column(name="reject_reason", length=20)
    private String rejectReason;

    @Column(name="nav_value")
    private BigDecimal navValue;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ValuationStatus status;

   @Column(name = "redis_sent", nullable = false, columnDefinition = "boolean default false")
    private Boolean redisSent = false;

    public Boolean getRedisSent() { return redisSent; }
    public void setRedisSent(Boolean redisSent) { this.redisSent = redisSent; }

}
