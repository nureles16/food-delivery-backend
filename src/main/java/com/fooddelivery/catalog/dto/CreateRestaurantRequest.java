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
    @NotBlank
    private String address;

    @Schema(
            description = "Город",
            example = "Bishkek"
    )
    @NotBlank
    private String city;

    @Schema(
            description = "URL логотипа",
            example = "https://cdn.fooddelivery.kg/logo.png"
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
    @NotNull
    private BigDecimal minOrderAmount;

    @Schema(
            description = "Радиус доставки (км)",
            example = "5"
    )
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