package com.fooddelivery.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Item inside order")
public class OrderItemDto {

    @Schema(description = "Menu item ID", example = "4f4d9f6b-9a1a-4e90-8a3f-111111111111", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ItemId обязателен")
    private UUID itemId;

    @Schema(description = "Quantity of item", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Количество обязателено")
    @Min(value = 1, message = "Количество должно быть хотя бы 1")
    private Integer quantity;

    public UUID getItemId() {
        return itemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}