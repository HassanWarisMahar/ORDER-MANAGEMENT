package com.microservices.inventoryservice.service;

import com.microservices.inventoryservice.dto.DecreaseStockRequest;
import com.microservices.inventoryservice.dto.DecreaseStockResponse;
import com.microservices.inventoryservice.dto.InventoryStockResponse;
import com.microservices.inventoryservice.model.InventoryItem;
import com.microservices.inventoryservice.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryItem inventoryItem;

    @BeforeEach
    void setUp() {
        inventoryItem = new InventoryItem();
        inventoryItem.setId(1L);
        inventoryItem.setItemCode("ITEM-001");
        inventoryItem.setItemName("Test Item");
        inventoryItem.setAvailableStock(10);
    }

    @Test
    void testGetAvailableStock_Success() {
        when(inventoryRepository.findByItemCode("ITEM-001")).thenReturn(Optional.of(inventoryItem));

        InventoryStockResponse response = inventoryService.getAvailableStock("ITEM-001");

        assertNotNull(response);
        assertEquals("ITEM-001", response.getItemCode());
        assertEquals(10, response.getAvailableStock());
        assertTrue(response.getInStock());
        verify(inventoryRepository, times(1)).findByItemCode("ITEM-001");
    }

    @Test
    void testGetAvailableStock_NotFound() {
        when(inventoryRepository.findByItemCode("ITEM-999")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> inventoryService.getAvailableStock("ITEM-999"));
    }

    @Test
    void testDecreaseStock_Success() {
        DecreaseStockRequest request = new DecreaseStockRequest("ITEM-001", 3);
        when(inventoryRepository.findByItemCode("ITEM-001")).thenReturn(Optional.of(inventoryItem));
        when(inventoryRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);

        DecreaseStockResponse response = inventoryService.decreaseStock(request);

        assertNotNull(response);
        assertEquals("ITEM-001", response.getItemCode());
        assertEquals(3, response.getQuantityDecreased());
        assertTrue(response.getSuccess());
        verify(inventoryRepository, times(1)).save(any(InventoryItem.class));
    }

    @Test
    void testDecreaseStock_InsufficientStock() {
        DecreaseStockRequest request = new DecreaseStockRequest("ITEM-001", 15);
        when(inventoryRepository.findByItemCode("ITEM-001")).thenReturn(Optional.of(inventoryItem));

        assertThrows(RuntimeException.class, () -> inventoryService.decreaseStock(request));
        verify(inventoryRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    void testDecreaseStock_ItemNotFound() {
        DecreaseStockRequest request = new DecreaseStockRequest("ITEM-999", 5);
        when(inventoryRepository.findByItemCode("ITEM-999")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> inventoryService.decreaseStock(request));
    }
}
