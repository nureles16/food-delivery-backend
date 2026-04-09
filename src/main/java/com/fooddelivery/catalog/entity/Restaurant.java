package com.fooddelivery.catalog.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    private String logoUrl;

    @Enumerated(EnumType.STRING)
    private CuisineType cuisineType;

    @Column(nullable = false)
    private BigDecimal minOrderAmount;

    private Double deliveryZoneRadiusKm;

    private boolean isActive = true;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkingHours> workingHours = new ArrayList<>();

    private Double latitude;
    private Double longitude;

    private String coverUrl;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    private boolean isVerified = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> paymentDetails;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> certificates;

    @Column(precision = 5, scale = 2)
    private BigDecimal commissionRate = new BigDecimal("12.00");

    @Column(precision = 10, scale = 2)
    private BigDecimal deliveryBaseFee = BigDecimal.valueOf(50);

    @Column(precision = 10, scale = 2)
    private BigDecimal deliveryFeePerKm = BigDecimal.valueOf(20);

    @Column(precision = 10, scale = 2)
    private BigDecimal freeDeliveryThreshold = BigDecimal.valueOf(500);

    public BigDecimal getDeliveryBaseFee() { return deliveryBaseFee; }
    public void setDeliveryBaseFee(BigDecimal deliveryBaseFee) { this.deliveryBaseFee = deliveryBaseFee; }

    public BigDecimal getDeliveryFeePerKm() { return deliveryFeePerKm; }
    public void setDeliveryFeePerKm(BigDecimal deliveryFeePerKm) { this.deliveryFeePerKm = deliveryFeePerKm; }

    public BigDecimal getFreeDeliveryThreshold() { return freeDeliveryThreshold; }
    public void setFreeDeliveryThreshold(BigDecimal freeDeliveryThreshold) { this.freeDeliveryThreshold = freeDeliveryThreshold; }

    public Restaurant() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public CuisineType getCuisineType() {
        return cuisineType;
    }

    public void setCuisineType(CuisineType cuisineType) {
        this.cuisineType = cuisineType;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public Double getDeliveryZoneRadiusKm() {
        return deliveryZoneRadiusKm;
    }

    public void setDeliveryZoneRadiusKm(Double deliveryZoneRadiusKm) {
        this.deliveryZoneRadiusKm = deliveryZoneRadiusKm;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public List<WorkingHours> getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(List<WorkingHours> workingHours) {
        this.workingHours = workingHours;
    }

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

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
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

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
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

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public boolean isOpenNow() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek today = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();
        return workingHours.stream()
                .filter(wh -> wh.getDayOfWeek() == today)
                .anyMatch(wh -> !currentTime.isBefore(wh.getOpenTime()) && !currentTime.isAfter(wh.getCloseTime()));
    }
}