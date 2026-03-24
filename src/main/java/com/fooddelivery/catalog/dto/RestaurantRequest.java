package com.fooddelivery.catalog.dto;

import com.fooddelivery.catalog.entity.CuisineType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "DTO для создания ресторана")
public class RestaurantRequest {

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
    @Size(max = 2000)
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

    @Schema(description = "Широта", example = "42.8746")
    @NotNull
    private Double latitude;

    @Schema(description = "Долгота", example = "74.5698")
    @NotNull
    private Double longitude;

    @Schema(description = "Телефон", example = "+996312123456")
    @NotBlank
    @Pattern(regexp = "^\\+996\\d{9}$", message = "Формат +996XXXXXXXXX")
    private String phone;

    @Schema(description = "Email", example = "info@burgerhouse.kg")
    @NotBlank
    @Email
    private String email;

    @Schema(description = "URL обложки", example = "https://cdn.../cover.jpg")
    @Pattern(regexp = "^(http|https)://.*$")
    private String coverUrl;

    @Schema(description = "График работы")
    @NotNull
    private List<WorkingHoursDto> workingHours;

    private Map<String, Object> paymentDetails;
    private List<Map<String, Object>> certificates;

    public RestaurantRequest() {}

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String getAddress() { return address; }

    public String getCity() { return city; }

    public String getLogoUrl() { return logoUrl; }

    public CuisineType getCuisineType() { return cuisineType; }

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }

    public Double getDeliveryZoneRadiusKm() { return deliveryZoneRadiusKm; }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public List<WorkingHoursDto> getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(List<WorkingHoursDto> workingHours) {
        this.workingHours = workingHours;
    }

    public Map<String, Object> getPaymentDetails() {
        return paymentDetails;
    }

    public void setPaymentDetails(Map<String, Object> paymentDetails) {
        this.paymentDetails = paymentDetails;
    }

    public List<Map<String, Object>> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<Map<String, Object>> certificates) {
        this.certificates = certificates;
    }
}