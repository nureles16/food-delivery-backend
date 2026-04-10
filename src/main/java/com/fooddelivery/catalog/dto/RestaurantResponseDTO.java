package com.fooddelivery.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "DTO ответа с данными ресторана")
public class RestaurantResponseDTO {

    @Schema(description = "ID ресторана", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Slug (часть URL)", example = "burger-house-bishkek")
    private String slug;

    @Schema(description = "Название", example = "Burger House")
    private String name;

    @Schema(description = "Описание", example = "Лучшие бургеры в Бишкеке")
    private String description;

    @Schema(description = "Адрес", example = "ул. Киевская 120")
    private String address;

    @Schema(description = "Город", example = "Bishkek")
    private String city;

    @Schema(description = "URL логотипа")
    private String logoUrl;

    @Schema(description = "URL обложки")
    private String coverUrl;

    @Schema(description = "Тип кухни")
    private String cuisineType;

    @Schema(description = "Минимальная сумма заказа", example = "300")
    private BigDecimal minOrderAmount;

    @Schema(description = "Радиус доставки (км)", example = "5")
    private Double deliveryZoneRadiusKm;

    @Schema(description = "Широта", example = "42.8746")
    private Double latitude;

    @Schema(description = "Долгота", example = "74.5698")
    private Double longitude;

    @Schema(description = "Телефон", example = "+996312123456")
    private String phone;

    @Schema(description = "Email", example = "info@burgerhouse.kg")
    private String email;

    @Schema(description = "Средний рейтинг", example = "4.7")
    private Double averageRating;

    @Schema(description = "Количество отзывов", example = "128")
    private Integer reviewCount;

    @Schema(description = "Активен ли", example = "true")
    private Boolean active;

    @Schema(description = "Открыт ли сейчас (расчётное поле)")
    private Boolean openNow;

    @Schema(description = "График работы")
    private List<WorkingHoursDto> workingHours;

    public RestaurantResponseDTO() {}


    public void setId(UUID id) {
        this.id = id;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public void setCuisineType(String cuisineType) {
        this.cuisineType = cuisineType;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public void setDeliveryZoneRadiusKm(Double deliveryZoneRadiusKm) {
        this.deliveryZoneRadiusKm = deliveryZoneRadiusKm;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setOpenNow(Boolean openNow) {
        this.openNow = openNow;
    }

    public void setWorkingHours(List<WorkingHoursDto> workingHours) {
        this.workingHours = workingHours;
    }
}