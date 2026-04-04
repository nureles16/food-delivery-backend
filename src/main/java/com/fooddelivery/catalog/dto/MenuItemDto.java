package com.fooddelivery.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "MenuItemDto", description = "DTO для создания/обновления позиции меню")
public class MenuItemDto {

    @Schema(description = "ID категории меню", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "CategoryId обязателен")
    private UUID categoryId;

    @Schema(description = "Название позиции меню", example = "Чизбургер", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Название обязательно")
    @Size(min = 2, max = 150)
    private String name;

    @Schema(description = "Описание позиции меню", example = "Сочный чизбургер с сыром")
    @Size(max = 1000)
    private String description;

    @Schema(description = "Цена позиции меню", example = "450.50", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0.01", message = "Цена должна быть больше 0")
    private BigDecimal price;

    @Schema(description = "Ссылка на изображение", example = "https://example.com/image.jpg")
    @Pattern(
            regexp = "^(http|https)://.*$",
            message = "Некорректный URL изображения"
    )
    private String imageUrl;

    @Schema(description = "Доступна ли позиция для заказа", example = "true")
    private boolean isAvailable;

    private Integer weightGrams;
    private String allergens;
    private String tags;

    public MenuItemDto(UUID categoryId, String name, String description, BigDecimal price, String imageUrl, Integer weightGrams, String allergens, String tags) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.isAvailable = isAvailable;
        this.weightGrams = weightGrams;
        this.allergens = allergens;
        this.tags = tags;
    }

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

    public Integer getWeightGrams() {
        return weightGrams;
    }

    public void setWeightGrams(Integer weightGrams) {
        this.weightGrams = weightGrams;
    }

    public String getAllergens() {
        return allergens;
    }

    public void setAllergens(String allergens) {
        this.allergens = allergens;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}