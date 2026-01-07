package com.example.valuation.entity;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "valuation_inbox")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inbox {
	@Id
    private UUID eventId; // Same as primary key from payload data
	
	@NotNull(message = "Inbox payload can't be null!")
    @Column(name = "payload", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;  // JSON string order payload
	
	@NotNull(message = "Inbox status can't be blank or null!")
	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private InboxStatus status;
	
	@NotNull(message = "Created time of payload received in inbox can't be null!")
    @Column(name = "created_at")
    private LocalDateTime createdAt; // Time at which payload was received in inbox

}
