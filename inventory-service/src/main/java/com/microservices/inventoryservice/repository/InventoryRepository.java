package com.microservices.inventoryservice.repository;

import com.microservices.inventoryservice.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    
    Optional<InventoryItem> findByItemCode(String itemCode);
    
    boolean existsByItemCode(String itemCode);
}
