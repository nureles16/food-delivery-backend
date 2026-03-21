package com.fooddelivery.catalog.service;

import com.fooddelivery.catalog.dto.CreateRestaurantRequest;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.repository.RestaurantRepository;
import com.fooddelivery.catalog.specification.RestaurantSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public Page<Restaurant> getAllRestaurants(Pageable pageable,
                                              String name,
                                              String city,
                                              String cuisineType,
                                              BigDecimal minOrderAmount,
                                              Boolean active) {
        Specification<Restaurant> spec = Specification.where(RestaurantSpecification.nameContains(name))
                .and(RestaurantSpecification.cityEquals(city))
                .and(RestaurantSpecification.cuisineTypeEquals(cuisineType))
                .and(RestaurantSpecification.minOrderAmountGreaterThanOrEqual(minOrderAmount))
                .and(RestaurantSpecification.isActive(active));

        return restaurantRepository.findAll(spec, pageable);
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
        String baseSlug = name
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "") // убрать мусор
                .replaceAll("\\s+", "-");        // пробелы → -

        String slug = baseSlug;
        int count = 1;

        while (restaurantRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + count++;
        }

        return slug;
    }
}