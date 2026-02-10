package com.microservices.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO from Payment Service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String status;
    private LocalDateTime createdAt;
}
