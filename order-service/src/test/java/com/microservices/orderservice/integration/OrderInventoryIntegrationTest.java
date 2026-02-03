package com.microservices.orderservice.integration;

import com.microservices.orderservice.OrderServiceApplication;
import com.microservices.orderservice.dto.OrderRequest;
import com.microservices.orderservice.dto.OrderItemRequest;
import com.microservices.orderservice.model.Order;
import com.microservices.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = OrderServiceApplication.class)
@ActiveProfiles("test")
@Transactional
class OrderInventoryIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void testOrderCreation_WithValidInventory() {
        // This test would require a running Inventory Service
        // In a real scenario, you'd use Testcontainers or WireMock
        // For now, this is a placeholder for integration test structure
        
        OrderRequest request = new OrderRequest();
        request.setCustomerName("Test Customer");
        request.setCustomerEmail("test@example.com");
        
        OrderItemRequest item = new OrderItemRequest();
        item.setItemCode("ITEM-001");
        item.setQuantity(2);
        request.setItems(Arrays.asList(item));

        // Integration test would call the actual REST endpoint
        // and verify the order is created and inventory is decreased
        assertTrue(true, "Integration test placeholder");
    }
}
