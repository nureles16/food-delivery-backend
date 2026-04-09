package com.fooddelivery.mapper;

import com.fooddelivery.catalog.dto.CreateCategoryRequest;
import com.fooddelivery.catalog.entity.MenuCategory;
import com.fooddelivery.catalog.entity.Restaurant;
import org.springframework.stereotype.Component;

@Component
public class MenuCategoryMapper {

    public MenuCategory toEntity(CreateCategoryRequest request, Restaurant restaurant) {
        if (request == null) {
            return null;
        }
        MenuCategory category = new MenuCategory();
        category.setName(request.getName().trim());
        category.setPosition(request.getPosition() != null ? request.getPosition() : 999);
        category.setActive(request.getActive() != null ? request.getActive() : true);
        category.setRestaurant(restaurant);
        return category;
    }

    public void updateEntity(MenuCategory category, CreateCategoryRequest request) {
        if (category == null || request == null) {
            return;
        }
        if (request.getName() != null) {
            category.setName(request.getName().trim());
        }
        if (request.getPosition() != null) {
            category.setPosition(request.getPosition());
        }
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
    }
}