package com.fooddelivery.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "DTO для создания или обновления категории меню")
public class CreateCategoryRequest {

    @Schema(description = "Название категории", example = "Burgers", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Название обязательно")
    @Size(min = 2, max = 100)
    private String name;

    @Schema(description = "ID ресторана, к которому относится категория", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private UUID restaurantId;

    @NotNull
    private Boolean isActive;

    @Schema(description = "Позиция категории в списке меню", example = "1")
    @Min(value = 0, message = "Позиция не может быть отрицательной")
    private Integer position;

    public CreateCategoryRequest(String name, UUID restaurantId, Boolean isActive, Integer position) {
        this.name = name;
        this.restaurantId = restaurantId;
        this.isActive = isActive;
        this.position = position;
    }

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

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
}