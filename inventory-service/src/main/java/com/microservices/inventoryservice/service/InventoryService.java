package com.microservices.inventoryservice.service;

import com.microservices.inventoryservice.dto.AddStockRequest;
import com.microservices.inventoryservice.dto.AddStockResponse;
import com.microservices.inventoryservice.dto.DecreaseStockRequest;
import com.microservices.inventoryservice.dto.DecreaseStockResponse;
import com.microservices.inventoryservice.dto.InventoryStockResponse;
import com.microservices.inventoryservice.model.InventoryItem;
import com.microservices.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public InventoryStockResponse getAvailableStock(String itemCode) {
        log.info("Fetching available stock for item code: {}", itemCode);
        
        InventoryItem item = inventoryRepository.findByItemCode(itemCode)
                .orElseThrow(() -> new RuntimeException("Inventory item not found with code: " + itemCode));
        
        InventoryStockResponse response = new InventoryStockResponse();
        response.setItemCode(item.getItemCode());
        response.setItemName(item.getItemName());
        response.setAvailableStock(item.getAvailableStock());
        response.setInStock(item.getAvailableStock() > 0);
        
        log.info("Stock retrieved for item {}: {} units available", itemCode, item.getAvailableStock());
        return response;
    }

    /**
     * Atomically decrease stock. Uses a conditional UPDATE in the database so that
     * under concurrent requests (e.g. two users ordering the last unit), only one succeeds.
     * Prevents overselling without distributed locks.
     */
    @Transactional
    public DecreaseStockResponse decreaseStock(DecreaseStockRequest request) {
        log.info("Decreasing stock for item code: {} by quantity: {} (atomic)", request.getItemCode(), request.getQuantity());

        // Ensure item exists
        inventoryRepository.findByItemCode(request.getItemCode())
                .orElseThrow(() -> new RuntimeException("Inventory item not found with code: " + request.getItemCode()));

        int rowsUpdated = inventoryRepository.decreaseStockIfAvailable(request.getItemCode(), request.getQuantity());

        if (rowsUpdated == 0) {
            InventoryItem item = inventoryRepository.findByItemCode(request.getItemCode()).orElseThrow();
            log.warn("Insufficient stock for item {}. Available: {}, Requested: {}",
                    request.getItemCode(), item.getAvailableStock(), request.getQuantity());
            throw new RuntimeException("Insufficient stock for item: " + request.getItemCode() +
                    ". Available: " + item.getAvailableStock() + ", Requested: " + request.getQuantity());
        }

        InventoryItem updatedItem = inventoryRepository.findByItemCode(request.getItemCode()).orElseThrow();
        log.info("Stock decreased for item {}. Remaining stock: {}", request.getItemCode(), updatedItem.getAvailableStock());

        DecreaseStockResponse response = new DecreaseStockResponse();
        response.setItemCode(updatedItem.getItemCode());
        response.setItemName(updatedItem.getItemName());
        response.setQuantityDecreased(request.getQuantity());
        response.setRemainingStock(updatedItem.getAvailableStock());
        response.setSuccess(true);

        return response;
    }

    @Transactional
    public AddStockResponse addStock(AddStockRequest request) {
        log.info("Adding stock for item code: {} by quantity: {}", request.getItemCode(), request.getQuantity());

        InventoryItem item = inventoryRepository.findByItemCode(request.getItemCode())
                .orElseThrow(() -> new RuntimeException("Inventory item not found with code: " + request.getItemCode()));

        int newStock = item.getAvailableStock() + request.getQuantity();
        item.setAvailableStock(newStock);
        InventoryItem updatedItem = inventoryRepository.save(item);

        log.info("Stock increased for item {}. New stock: {}", request.getItemCode(), newStock);

        AddStockResponse response = new AddStockResponse();
        response.setItemCode(updatedItem.getItemCode());
        response.setItemName(updatedItem.getItemName());
        response.setQuantityAdded(request.getQuantity());
        response.setUpdatedStock(updatedItem.getAvailableStock());
        response.setSuccess(true);

        return response;
    }
}
