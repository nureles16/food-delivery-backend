package com.fooddelivery.orders.dto;


public class TrackingInfoDto {
    private String deliveryStatus;
    private String trackingUrl;
    private String estimatedDeliveryAt;

    public TrackingInfoDto(String deliveryStatus, String trackingUrl, String estimatedDeliveryAt) {
        this.deliveryStatus = deliveryStatus;
        this.trackingUrl = trackingUrl;
        this.estimatedDeliveryAt = estimatedDeliveryAt;
    }
}