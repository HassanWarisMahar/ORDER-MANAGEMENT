package com.microservices.orderservice.service;

import com.microservices.orderservice.dto.*;
import com.microservices.orderservice.model.Order;
import com.microservices.orderservice.model.OrderItem;
import com.microservices.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient inventoryWebClient;
    private final WebClient paymentWebClient;

    /**
     * Create order only after stock is atomically reserved (decreased) in Inventory.
     * This prevents overselling when multiple users order the same last unit:
     * - Inventory uses conditional UPDATE (stock only decreased if stock >= quantity).
     * - One request succeeds; others get 409 Insufficient stock and no order is created.
     * For multi-item orders, if a later item fails we compensate (add back) earlier decreases.
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerEmail());

        // 1. Reserve stock first (atomic decrease). On any failure, compensate and rethrow.
        java.util.List<OrderItemRequest> items = request.getItems();
        int succeeded = 0;
        try {
            for (OrderItemRequest itemRequest : items) {
                decreaseItemStock(itemRequest.getItemCode(), itemRequest.getQuantity());
                succeeded++;
            }
        } catch (RuntimeException e) {
            // Compensate: add back stock for items we already decreased
            for (int i = 0; i < succeeded; i++) {
                OrderItemRequest itemRequest = items.get(i);
                try {
                    addItemStock(itemRequest.getItemCode(), itemRequest.getQuantity());
                    log.info("Compensated stock for item: {} (+{})", itemRequest.getItemCode(), itemRequest.getQuantity());
                } catch (Exception ex) {
                    log.error("Compensation failed for item {}: {}", itemRequest.getItemCode(), ex.getMessage());
                }
            }
            throw e;
        }

        // 2. Build and persist order (stock already reserved)
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setStatus(Order.OrderStatus.CONFIRMED);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : items) {
            InventoryStockResponse stockResponse = getItemStock(itemRequest.getItemCode());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setItemCode(itemRequest.getItemCode());
            item.setItemName(stockResponse.getItemName());
            item.setQuantity(itemRequest.getQuantity());
            BigDecimal price = BigDecimal.valueOf(10.00);
            item.setPrice(price);

            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            item.setSubtotal(subtotal);

            totalAmount = totalAmount.add(subtotal);
            order.getItems().add(item);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        createPaymentForOrder(savedOrder);
        log.info("Order created successfully with ID: {}", savedOrder.getId());
        return OrderResponse.fromEntity(savedOrder);
    }

    private void decreaseItemStock(String itemCode, Integer quantity) {
        log.info("Reserving stock for item: {}, quantity: {}", itemCode, quantity);
        try {
            DecreaseStockRequest decreaseRequest = new DecreaseStockRequest(itemCode, quantity);
            DecreaseStockResponse response = inventoryWebClient
                    .post()
                    .uri("/api/inventory/decrease")
                    .bodyValue(decreaseRequest)
                    .retrieve()
                    .bodyToMono(DecreaseStockResponse.class)
                    .block();

            if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
                throw new RuntimeException("Failed to reserve stock for item: " + itemCode);
            }
            log.info("Stock reserved for item: {}", itemCode);
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("Item not found: " + itemCode);
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            if (e.getStatusCode() != null && e.getStatusCode().value() == 409) {
                throw new RuntimeException("Insufficient stock for item: " + itemCode + ". " + (body != null ? body : e.getMessage()));
            }
            throw new RuntimeException("Error reserving stock: " + e.getMessage());
        }
    }

    private void addItemStock(String itemCode, Integer quantity) {
        inventoryWebClient
                .post()
                .uri("/api/inventory/add")
                .bodyValue(new AddStockRequest(itemCode, quantity))
                .retrieve()
                .bodyToMono(AddStockResponse.class)
                .block();
    }
    
    private InventoryStockResponse getItemStock(String itemCode) {
        try {
            return inventoryWebClient
                    .get()
                    .uri("/api/inventory/{itemCode}", itemCode)
                    .retrieve()
                    .bodyToMono(InventoryStockResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error fetching item stock for: {}", itemCode, e);
            throw new RuntimeException("Error fetching item details: " + e.getMessage());
        }
    }

    private void createPaymentForOrder(Order order) {
        PaymentRequest paymentRequest = new PaymentRequest(
                order.getId(),
                order.getTotalAmount(),
                "USD",
                "CARD"
        );
        try {
            paymentWebClient
                    .post()
                    .uri("/api/payments")
                    .bodyValue(paymentRequest)
                    .retrieve()
                    .bodyToMono(PaymentResponse.class)
                    .block();
            log.info("Payment created for orderId={}", order.getId());
        } catch (WebClientResponseException e) {
            log.error("Error creating payment for orderId={}", order.getId(), e);
            throw new RuntimeException("Error creating payment: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return OrderResponse.fromEntity(order);
    }

}
