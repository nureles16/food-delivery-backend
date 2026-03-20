package com.fooddelivery.catalog.dto;

import com.fooddelivery.catalog.entity.CuisineType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "DTO для создания ресторана")
public class CreateRestaurantRequest {

    @Schema(
            description = "Название ресторана",
            example = "Burger House"
    )
    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @Schema(
            description = "Описание ресторана",
            example = "Лучшие бургеры в Бишкеке"
    )
    private String description;

    @Schema(
            description = "Адрес ресторана",
            example = "ул. Киевская 120"
    )
    @NotBlank(message = "Адрес обязателен")
    @Size(max = 255)
    private String address;

    @Schema(
            description = "Город",
            example = "Bishkek"
    )
    @NotBlank(message = "Город обязателен")
    @Size(max = 100)
    private String city;

    @Schema(
            description = "URL логотипа",
            example = "https://cdn.fooddelivery.kg/logo.png"
    )
    @Pattern(
            regexp = "^(http|https)://.*$",
            message = "Некорректный URL логотипа"
    )
    private String logoUrl;

    @Schema(
            description = "Тип кухни",
            example = "BURGERS"
    )
    @NotNull
    private CuisineType cuisineType;

    @Schema(
            description = "Минимальная сумма заказа",
            example = "300"
    )
    @NotNull(message = "Минимальная сумма обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Должно быть больше 0")
    private BigDecimal minOrderAmount;

    @Schema(
            description = "Радиус доставки (км)",
            example = "5"
    )
    @DecimalMin(value = "0.0", inclusive = false, message = "Радиус должен быть больше 0")
    private Double deliveryZoneRadiusKm;

    public CreateRestaurantRequest() {}

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String getAddress() { return address; }

    public String getCity() { return city; }

    public String getLogoUrl() { return logoUrl; }

    public CuisineType getCuisineType() { return cuisineType; }

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }

    public Double getDeliveryZoneRadiusKm() { return deliveryZoneRadiusKm; }

}