package com.microservices.paymentservice.service;

import com.microservices.paymentservice.dto.PaymentRequest;
import com.microservices.paymentservice.dto.PaymentResponse;
import com.microservices.paymentservice.model.Payment;
import com.microservices.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Creating payment for orderId={}, amount={} {}", request.getOrderId(), request.getAmount(), request.getCurrency());

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(Payment.PaymentStatus.COMPLETED)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created with id={} for orderId={}", saved.getId(), saved.getOrderId());
        return PaymentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(PaymentResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("Payment not found for order id: " + orderId));
    }
}
