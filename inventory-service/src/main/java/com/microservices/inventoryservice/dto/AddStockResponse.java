package com.microservices.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddStockResponse {

    private String itemCode;
    private String itemName;
    private Integer quantityAdded;
    private Integer updatedStock;
    private Boolean success;
}
