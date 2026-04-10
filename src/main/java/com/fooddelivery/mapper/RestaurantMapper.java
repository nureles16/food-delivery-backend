package com.fooddelivery.mapper;

import com.fooddelivery.catalog.dto.RestaurantRequest;
import com.fooddelivery.catalog.dto.RestaurantResponseDTO;
import com.fooddelivery.catalog.dto.WorkingHoursDto;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.entity.WorkingHours;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.stream.Collectors;

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

    public RestaurantResponseDTO toResponseDTO(Restaurant restaurant) {
        if (restaurant == null) return null;
        RestaurantResponseDTO dto = new RestaurantResponseDTO();
        dto.setId(restaurant.getId());
        dto.setSlug(restaurant.getSlug());
        dto.setName(restaurant.getName());
        dto.setDescription(restaurant.getDescription());
        dto.setAddress(restaurant.getAddress());
        dto.setCity(restaurant.getCity());
        dto.setLogoUrl(restaurant.getLogoUrl());
        dto.setCoverUrl(restaurant.getCoverUrl());
        dto.setCuisineType(restaurant.getCuisineType() != null ? restaurant.getCuisineType().name() : null);
        dto.setMinOrderAmount(restaurant.getMinOrderAmount());
        dto.setDeliveryZoneRadiusKm(restaurant.getDeliveryZoneRadiusKm());
        dto.setLatitude(restaurant.getLatitude());
        dto.setLongitude(restaurant.getLongitude());
        dto.setPhone(restaurant.getPhone());
        dto.setEmail(restaurant.getEmail());
        dto.setOpenNow(restaurant.isOpenNow());

        if (restaurant.getWorkingHours() != null) {
            dto.setWorkingHours(restaurant.getWorkingHours().stream()
                    .map(this::toWorkingHoursResponseDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private WorkingHoursDto toWorkingHoursResponseDTO(WorkingHours wh) {
        if (wh == null) return null;
        WorkingHoursDto dto = new WorkingHoursDto();
        dto.setDayOfWeek(wh.getDayOfWeek() != null ? DayOfWeek.valueOf(wh.getDayOfWeek().name()) : null);
        dto.setOpenTime(wh.getOpenTime() != null ? LocalTime.parse(wh.getOpenTime().toString()) : null);
        dto.setCloseTime(wh.getCloseTime() != null ? LocalTime.parse(wh.getCloseTime().toString()) : null);
        return dto;
    }
}