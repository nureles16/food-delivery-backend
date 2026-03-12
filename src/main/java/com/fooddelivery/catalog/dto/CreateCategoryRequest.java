package com.fooddelivery.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "DTO для создания или обновления категории меню")
public class CreateCategoryRequest {

    @Schema(description = "Название категории", example = "Burgers", required = true)
    @NotBlank
    private String name;

    @Schema(description = "ID ресторана, к которому относится категория", required = true)
    @NotNull
    private UUID restaurantId;

    @Schema(description = "Позиция категории в списке меню", example = "1")
    private Integer position;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}