package com.fooddelivery.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "MenuItemDto", description = "DTO для создания/обновления позиции меню")
public class MenuItemDto {

    @Schema(description = "ID категории меню", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    private UUID categoryId;

    @Schema(description = "Название позиции меню", example = "Чизбургер", required = true)
    private String name;

    @Schema(description = "Описание позиции меню", example = "Сочный чизбургер с сыром")
    private String description;

    @Schema(description = "Цена позиции меню", example = "450.50", required = true)
    private BigDecimal price;

    @Schema(description = "Ссылка на изображение", example = "https://example.com/image.jpg")
    private String imageUrl;

    @Schema(description = "Доступна ли позиция для заказа", example = "true")
    private boolean isAvailable;

    // Getters и Setters
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
}