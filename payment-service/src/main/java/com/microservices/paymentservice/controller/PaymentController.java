package com.microservices.paymentservice.controller;

import com.microservices.paymentservice.dto.PaymentRequest;
import com.microservices.paymentservice.dto.PaymentResponse;
import com.microservices.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "APIs for creating and retrieving payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Create payment", description = "Creates a payment record for an order")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order ID", description = "Retrieves the payment for a given order")
    public ResponseEntity<PaymentResponse> getByOrderId(@PathVariable Long orderId) {
        PaymentResponse response = paymentService.getByOrderId(orderId);
        return ResponseEntity.ok(response);
    }
}
