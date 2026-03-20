package com.fooddelivery.catalog.repository;

import com.fooddelivery.catalog.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    Page<MenuItem> findAllByCategoryId(UUID categoryId, Pageable pageable);
}