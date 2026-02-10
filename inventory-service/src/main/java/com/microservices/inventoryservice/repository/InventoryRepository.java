package com.microservices.inventoryservice.repository;

import com.microservices.inventoryservice.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByItemCode(String itemCode);

    boolean existsByItemCode(String itemCode);

    /**
     * Atomically decrease stock only if current stock >= quantity.
     * Prevents overselling under concurrent order submissions.
     *
     * @return number of rows updated (1 = success, 0 = insufficient stock)
     */
    @Modifying
    @Query("UPDATE InventoryItem i SET i.availableStock = i.availableStock - :quantity WHERE i.itemCode = :itemCode AND i.availableStock >= :quantity")
    int decreaseStockIfAvailable(@Param("itemCode") String itemCode, @Param("quantity") Integer quantity);
}
