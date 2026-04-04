package com.fooddelivery.catalog.dto;


import java.util.List;
import java.util.UUID;

public class CategoryWithItemsDto {
    private UUID id;

    private String name;

    private Integer position;

    private List<MenuItemDto> menuItems;

    public CategoryWithItemsDto(UUID id, String name, Integer position, List<MenuItemDto> menuItems) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.menuItems = menuItems;
    }
}
