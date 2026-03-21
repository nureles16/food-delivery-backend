package com.fooddelivery.catalog.repository;

import com.fooddelivery.catalog.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID>, JpaSpecificationExecutor<MenuCategory> {
        List<MenuCategory> findByRestaurantId(UUID restaurantId);

}
