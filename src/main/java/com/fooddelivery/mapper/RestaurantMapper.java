package com.fooddelivery.mapper;

import com.fooddelivery.catalog.dto.RestaurantRequest;
import com.fooddelivery.catalog.dto.WorkingHoursDto;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.entity.WorkingHours;
import org.springframework.stereotype.Component;

@Component
public class RestaurantMapper {

    public Restaurant toEntity(RestaurantRequest request) {
        if (request == null) {
            return null;
        }
        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setCity(request.getCity());
        restaurant.setLogoUrl(request.getLogoUrl());
        restaurant.setCoverUrl(request.getCoverUrl());
        restaurant.setCuisineType(request.getCuisineType());
        restaurant.setMinOrderAmount(request.getMinOrderAmount());
        restaurant.setDeliveryZoneRadiusKm(request.getDeliveryZoneRadiusKm());
        restaurant.setLatitude(request.getLatitude());
        restaurant.setLongitude(request.getLongitude());
        restaurant.setPhone(request.getPhone());
        restaurant.setEmail(request.getEmail());
        restaurant.setPaymentDetails(request.getPaymentDetails());
        restaurant.setCertificates(request.getCertificates());
        return restaurant;
    }

    public void updateEntity(Restaurant restaurant, RestaurantRequest request) {
        if (restaurant == null || request == null) {
            return;
        }
        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setCity(request.getCity());
        restaurant.setLogoUrl(request.getLogoUrl());
        restaurant.setCoverUrl(request.getCoverUrl());
        restaurant.setCuisineType(request.getCuisineType());
        restaurant.setMinOrderAmount(request.getMinOrderAmount());
        restaurant.setDeliveryZoneRadiusKm(request.getDeliveryZoneRadiusKm());
        restaurant.setLatitude(request.getLatitude());
        restaurant.setLongitude(request.getLongitude());
        restaurant.setPhone(request.getPhone());
        restaurant.setEmail(request.getEmail());
        restaurant.setPaymentDetails(request.getPaymentDetails());
        restaurant.setCertificates(request.getCertificates());
    }

    public WorkingHours toWorkingHours(WorkingHoursDto dto, Restaurant restaurant) {
        if (dto == null) {
            return null;
        }
        WorkingHours wh = new WorkingHours();
        wh.setRestaurant(restaurant);
        wh.setDayOfWeek(dto.getDayOfWeek());
        wh.setOpenTime(dto.getOpenTime());
        wh.setCloseTime(dto.getCloseTime());
        return wh;
    }
}