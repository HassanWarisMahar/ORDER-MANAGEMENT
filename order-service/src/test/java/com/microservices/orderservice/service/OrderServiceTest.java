package com.microservices.orderservice.service;

import com.microservices.orderservice.dto.*;
import com.microservices.orderservice.model.Order;
import com.microservices.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private WebClient inventoryWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private OrderService orderService;

    private OrderRequest orderRequest;
    private Order order;
    private InventoryStockResponse stockResponse;

    @BeforeEach
    void setUp() {
        orderRequest = new OrderRequest();
        orderRequest.setCustomerName("John Doe");
        orderRequest.setCustomerEmail("john@example.com");
        
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setItemCode("ITEM-001");
        item1.setQuantity(2);
        
        orderRequest.setItems(Arrays.asList(item1));

        order = new Order();
        order.setId(1L);
        order.setCustomerName("John Doe");
        order.setCustomerEmail("john@example.com");
        order.setTotalAmount(new BigDecimal("20.00"));
        order.setStatus(Order.OrderStatus.CONFIRMED);

        stockResponse = new InventoryStockResponse();
        stockResponse.setItemCode("ITEM-001");
        stockResponse.setItemName("Test Item");
        stockResponse.setAvailableStock(10);
        stockResponse.setInStock(true);
    }

    @Test
    void testCreateOrder_Success() {
        // Mock WebClient calls for stock validation
        when(inventoryWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(InventoryStockResponse.class))
                .thenReturn(Mono.just(stockResponse));

        // Mock WebClient call for stock decrease
        when(inventoryWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        DecreaseStockResponse decreaseResponse = new DecreaseStockResponse();
        decreaseResponse.setSuccess(true);
        when(responseSpec.bodyToMono(DecreaseStockResponse.class))
                .thenReturn(Mono.just(decreaseResponse));

        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.createOrder(orderRequest);

        assertNotNull(response);
        assertEquals("John Doe", response.getCustomerName());
        assertEquals(Order.OrderStatus.CONFIRMED, response.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateOrder_InsufficientStock() {
        stockResponse.setAvailableStock(1);
        
        when(inventoryWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(InventoryStockResponse.class))
                .thenReturn(Mono.just(stockResponse));

        assertThrows(RuntimeException.class, () -> orderService.createOrder(orderRequest));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testGetOrderById_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void testGetOrderById_NotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> orderService.getOrderById(1L));
    }
}
