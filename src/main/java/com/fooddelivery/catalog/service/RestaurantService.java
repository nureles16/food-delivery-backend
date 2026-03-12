package com.fooddelivery.catalog.service;

import com.fooddelivery.catalog.dto.CreateRestaurantRequest;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.repository.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public Restaurant createRestaurant(CreateRestaurantRequest request) {

        Restaurant restaurant = new Restaurant();

        restaurant.setName(request.getName());
        restaurant.setSlug(generateSlug(request.getName()));
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setCity(request.getCity());
        restaurant.setLogoUrl(request.getLogoUrl());
        restaurant.setCuisineType(request.getCuisineType());
        restaurant.setMinOrderAmount(request.getMinOrderAmount());
        restaurant.setDeliveryZoneRadiusKm(request.getDeliveryZoneRadiusKm());

        return restaurantRepository.save(restaurant);
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    public Restaurant getRestaurant(UUID id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
    }

    public Restaurant updateRestaurant(UUID id, CreateRestaurantRequest request) {

        Restaurant restaurant = getRestaurant(id);

        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setCity(request.getCity());
        restaurant.setLogoUrl(request.getLogoUrl());
        restaurant.setCuisineType(request.getCuisineType());
        restaurant.setMinOrderAmount(request.getMinOrderAmount());
        restaurant.setDeliveryZoneRadiusKm(request.getDeliveryZoneRadiusKm());

        return restaurantRepository.save(restaurant);
    }

    private String generateSlug(String name) {
        return name.toLowerCase().replace(" ", "-");
    }
}