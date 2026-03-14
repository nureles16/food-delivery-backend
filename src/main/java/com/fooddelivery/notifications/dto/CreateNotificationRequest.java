package com.fooddelivery.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "DTO для создания уведомления")
public class CreateNotificationRequest {

    @NotNull(message = "UserId cannot be null")
    @Schema(description = "ID пользователя, которому отправляется уведомление", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    private UUID userId;

    @NotBlank(message = "Title cannot be blank")
    @Schema(description = "Заголовок уведомления", example = "Новый заказ", required = true)
    private String title;

    @NotBlank(message = "Message cannot be blank")
    @Schema(description = "Текст уведомления", example = "Ваш заказ #123 готов к доставке", required = true)
    private String message;

    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}