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

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerEmail());
        
        // Validate and check stock availability for all items
        for (OrderItemRequest itemRequest : request.getItems()) {
            validateItemAvailability(itemRequest.getItemCode(), itemRequest.getQuantity());
        }
        
        // Decrease stock for all items
        for (OrderItemRequest itemRequest : request.getItems()) {
            decreaseItemStock(itemRequest.getItemCode(), itemRequest.getQuantity());
        }
        
        // Create order
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setStatus(Order.OrderStatus.CONFIRMED);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            // Fetch item details from inventory
            InventoryStockResponse stockResponse = getItemStock(itemRequest.getItemCode());
            
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setItemCode(itemRequest.getItemCode());
            item.setItemName(stockResponse.getItemName());
            item.setQuantity(itemRequest.getQuantity());
            // Note: In a real scenario, price would come from inventory or a pricing service
            // For now, we'll use a default or fetch from inventory
            BigDecimal price = BigDecimal.valueOf(10.00); // Default price
            item.setPrice(price);
            
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            item.setSubtotal(subtotal);
            
            totalAmount = totalAmount.add(subtotal);
            order.getItems().add(item);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());
        return OrderResponse.fromEntity(savedOrder);
    }
    
    private void validateItemAvailability(String itemCode, Integer quantity) {
        log.info("Validating availability for item: {}, quantity: {}", itemCode, quantity);
        try {
            InventoryStockResponse response = inventoryWebClient
                    .get()
                    .uri("/api/inventory/{itemCode}", itemCode)
                    .retrieve()
                    .bodyToMono(InventoryStockResponse.class)
                    .block();
            
            if (response == null || response.getAvailableStock() < quantity) {
                throw new RuntimeException("Insufficient stock for item: " + itemCode + 
                        ". Available: " + (response != null ? response.getAvailableStock() : 0) + 
                        ", Requested: " + quantity);
            }
            log.info("Stock validation successful for item: {}", itemCode);
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("Item not found: " + itemCode);
        } catch (WebClientResponseException e) {
            log.error("Error validating stock for item: {}", itemCode, e);
            throw new RuntimeException("Error validating stock: " + e.getMessage());
        }
    }
    
    private void decreaseItemStock(String itemCode, Integer quantity) {
        log.info("Decreasing stock for item: {}, quantity: {}", itemCode, quantity);
        try {
            DecreaseStockRequest decreaseRequest = new DecreaseStockRequest(itemCode, quantity);
            DecreaseStockResponse response = inventoryWebClient
                    .post()
                    .uri("/api/inventory/decrease")
                    .bodyValue(decreaseRequest)
                    .retrieve()
                    .bodyToMono(DecreaseStockResponse.class)
                    .block();
            
            if (response == null || !response.getSuccess()) {
                throw new RuntimeException("Failed to decrease stock for item: " + itemCode);
            }
            log.info("Stock decreased successfully for item: {}", itemCode);
        } catch (WebClientResponseException e) {
            log.error("Error decreasing stock for item: {}", itemCode, e);
            throw new RuntimeException("Error decreasing stock: " + e.getMessage());
        }
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

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return OrderResponse.fromEntity(order);
    }

}
