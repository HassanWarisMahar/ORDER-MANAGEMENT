package com.microservices.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStockResponse {

    private String itemCode;
    private String itemName;
    private Integer availableStock;
    private Boolean inStock;
}
