package com.fooddelivery.catalog.repository;

import com.fooddelivery.catalog.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;


@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID>, JpaSpecificationExecutor<MenuItem> {
    boolean existsByCategoryId(UUID id);

    List<MenuItem> findByCategoryIdInAndIsAvailableTrue(List<UUID> categoryIds);
}