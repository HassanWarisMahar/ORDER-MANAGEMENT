package com.microservices.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for calling Payment Service to create a payment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    private Long orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
}
