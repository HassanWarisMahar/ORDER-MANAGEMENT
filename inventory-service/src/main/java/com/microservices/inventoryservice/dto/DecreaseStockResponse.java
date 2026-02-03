package com.microservices.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecreaseStockResponse {

    private String itemCode;
    private String itemName;
    private Integer quantityDecreased;
    private Integer remainingStock;
    private Boolean success;
}
