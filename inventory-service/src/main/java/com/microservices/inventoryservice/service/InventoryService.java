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

    @Transactional
    public DecreaseStockResponse decreaseStock(DecreaseStockRequest request) {
        log.info("Decreasing stock for item code: {} by quantity: {}", request.getItemCode(), request.getQuantity());
        
        InventoryItem item = inventoryRepository.findByItemCode(request.getItemCode())
                .orElseThrow(() -> new RuntimeException("Inventory item not found with code: " + request.getItemCode()));
        
        if (item.getAvailableStock() < request.getQuantity()) {
            log.warn("Insufficient stock for item {}. Available: {}, Requested: {}", 
                    request.getItemCode(), item.getAvailableStock(), request.getQuantity());
            throw new RuntimeException("Insufficient stock. Available: " + item.getAvailableStock() + 
                    ", Requested: " + request.getQuantity());
        }
        
        int newStock = item.getAvailableStock() - request.getQuantity();
        item.setAvailableStock(newStock);
        InventoryItem updatedItem = inventoryRepository.save(item);
        
        log.info("Stock decreased for item {}. Remaining stock: {}", request.getItemCode(), newStock);
        
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
