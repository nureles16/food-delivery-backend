package com.fooddelivery.mapper;

import com.fooddelivery.catalog.dto.MenuItemDto;
import com.fooddelivery.catalog.entity.MenuItem;
import org.springframework.stereotype.Component;

@Component
public class MenuItemMapper {

    public MenuItem toEntity(MenuItemDto dto) {
        if (dto == null) {
            return null;
        }
        MenuItem item = new MenuItem();
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setImageUrl(dto.getImageUrl());
        item.setAvailable(dto.isAvailable());
        item.setWeightGrams(dto.getWeightGrams());
        item.setAllergens(dto.getAllergens());
        item.setTags(dto.getTags());
        return item;
    }

    public void updateEntity(MenuItem item, MenuItemDto dto) {
        if (item == null || dto == null) {
            return;
        }
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setImageUrl(dto.getImageUrl());
        item.setAvailable(dto.isAvailable());
        item.setWeightGrams(dto.getWeightGrams());
        item.setAllergens(dto.getAllergens());
        item.setTags(dto.getTags());
    }

    public MenuItemDto toDto(MenuItem item) {
        if (item == null) {
            return null;
        }
        return new MenuItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getImageUrl(),
                item.getWeightGrams(),
                item.getAllergens(),
                item.getTags()
        );
    }
}