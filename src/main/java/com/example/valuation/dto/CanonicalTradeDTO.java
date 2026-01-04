package com.example.valuation.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CanonicalTradeDTO {

    private UUID id;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime tradeDateTime;
    private LocalDateTime validatedAt;

    private Integer originatorType;
    private Integer firmNumber;
    private Integer fundNumber;

    private String transactionType;
    private String transactionId;

    private UUID rawOrderId;
    private UUID fileId;

    private String orderSource;

    private BigDecimal dollarAmount;
    private Integer clientAccountNo;
    private String clientName;
    private String ssn;
    private LocalDate dob;
    private BigDecimal shareQuantity;

    private String validationErrors;
    private String requestId;
}

