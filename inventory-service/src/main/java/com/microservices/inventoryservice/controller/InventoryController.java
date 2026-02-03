package com.microservices.inventoryservice.controller;

import com.microservices.inventoryservice.dto.AddStockRequest;
import com.microservices.inventoryservice.dto.AddStockResponse;
import com.microservices.inventoryservice.dto.DecreaseStockRequest;
import com.microservices.inventoryservice.dto.DecreaseStockResponse;
import com.microservices.inventoryservice.dto.InventoryStockResponse;
import com.microservices.inventoryservice.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "APIs for managing inventory stock levels")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{itemCode}")
    @Operation(summary = "Get available stock", description = "Retrieves the available stock for a specific item code")
    public ResponseEntity<InventoryStockResponse> getAvailableStock(@PathVariable String itemCode) {
        InventoryStockResponse response = inventoryService.getAvailableStock(itemCode);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/decrease")
    @Operation(summary = "Decrease item stock", description = "Decreases the stock quantity for a specific item")
    public ResponseEntity<DecreaseStockResponse> decreaseStock(@Valid @RequestBody DecreaseStockRequest request) {
        DecreaseStockResponse response = inventoryService.decreaseStock(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/add")
    @Operation(summary = "Add item stock", description = "Increases the stock quantity for a specific item")
    public ResponseEntity<AddStockResponse> addStock(@Valid @RequestBody AddStockRequest request) {
        AddStockResponse response = inventoryService.addStock(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
