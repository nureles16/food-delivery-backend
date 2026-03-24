package com.fooddelivery.catalog.service;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.service.AuthService;
import com.fooddelivery.catalog.dto.RestaurantRequest;
import com.fooddelivery.catalog.dto.WorkingHoursDto;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.entity.WorkingHours;
import com.fooddelivery.catalog.repository.RestaurantRepository;
import com.fooddelivery.catalog.repository.WorkingHoursRepository;
import com.fooddelivery.catalog.specification.RestaurantSpecification;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);
    private final RestaurantRepository restaurantRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final AuthService authService;

    public RestaurantService(RestaurantRepository restaurantRepository,
                             WorkingHoursRepository workingHoursRepository, AuthService authService) {
        this.restaurantRepository = restaurantRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.authService = authService;
    }

    @Transactional
    public Restaurant createRestaurant(RestaurantRequest request) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != Role.SUPER_ADMIN) {
            log.warn("Access denied: user {} tried to create restaurant without SUPER_ADMIN role",
                    currentUser.getId());
            throw new AccessDeniedException("Only SUPER_ADMIN can create restaurants");
        }
        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.getName());
        restaurant.setSlug(generateSlug(request.getName()));
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
        restaurant.setActive(true);
        restaurant.setVerified(false);
        restaurant.setCommissionRate(new BigDecimal("12.00"));

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        if (request.getWorkingHours() != null && !request.getWorkingHours().isEmpty()) {
            List<WorkingHours> workingHoursList = request.getWorkingHours().stream()
                    .map(dto -> mapToWorkingHours(dto, savedRestaurant))
                    .collect(Collectors.toList());
            workingHoursRepository.saveAll(workingHoursList);
            savedRestaurant.setWorkingHours(workingHoursList);
        }

        return savedRestaurant;
    }

    @Transactional
    public Restaurant updateRestaurant(UUID id, RestaurantRequest request) {
        Restaurant restaurant = getRestaurant(id);
        User currentUser = authService.getCurrentUser();

        boolean isSuperAdmin = currentUser.getRole() == Role.SUPER_ADMIN;
        boolean isOwnerCafeAdmin = currentUser.getRole() == Role.CAFE_ADMIN
                && currentUser.getCafeId() != null
                && currentUser.getCafeId().equals(restaurant.getId());

        if (!isSuperAdmin && !isOwnerCafeAdmin) {
            throw new AccessDeniedException("You don't have permission to update this restaurant");
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

        if (request.getWorkingHours() != null) {
            workingHoursRepository.deleteByRestaurantId(restaurant.getId());
            List<WorkingHours> newWorkingHours = request.getWorkingHours().stream()
                    .map(dto -> mapToWorkingHours(dto, restaurant))
                    .collect(Collectors.toList());
            workingHoursRepository.saveAll(newWorkingHours);
            restaurant.setWorkingHours(newWorkingHours);
        }

        return restaurantRepository.save(restaurant);
    }

    public Page<Restaurant> getAllRestaurants(Pageable pageable,
                                              String name,
                                              String city,
                                              String cuisineType,
                                              BigDecimal minOrderAmount,
                                              Boolean active,
                                              Boolean openNow) {
        Specification<Restaurant> spec = Specification.where(RestaurantSpecification.nameContains(name))
                .and(RestaurantSpecification.cityEquals(city))
                .and(RestaurantSpecification.cuisineTypeEquals(cuisineType))
                .and(RestaurantSpecification.minOrderAmountGreaterThanOrEqual(minOrderAmount))
                .and(RestaurantSpecification.isActive(active));

        if (Boolean.TRUE.equals(openNow)) {
            spec = spec.and(RestaurantSpecification.isOpenNow());
        }

        return restaurantRepository.findAll(spec, pageable);
    }

    public Restaurant getRestaurant(UUID id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Restaurant not found: id={}, userId={}",
                            id, getCurrentUserIdSafely());
                    return new RuntimeException("Restaurant not found: " + id);
                });
    }

    public Restaurant getRestaurantBySlug(String slug) {
        return restaurantRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
    }

    @Transactional
    public void setRestaurantActive(UUID id, boolean active) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Only SUPER_ADMIN can change restaurant status");
        }
        Restaurant restaurant = getRestaurant(id);
        restaurant.setActive(active);
        restaurantRepository.save(restaurant);
    }

    @Transactional
    public void setRestaurantVerified(UUID id, boolean verified) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Only SUPER_ADMIN can verify restaurants");
        }
        Restaurant restaurant = getRestaurant(id);
        restaurant.setVerified(verified);
        restaurantRepository.save(restaurant);
    }

    private String generateSlug(String name) {
        String baseSlug = name
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");

        String slug = baseSlug;
        int count = 1;

        while (restaurantRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + count++;
        }

        return slug;
    }

    private String getCurrentUserIdSafely() {
        try {
            return authService.getCurrentUser().getId().toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private WorkingHours mapToWorkingHours(WorkingHoursDto dto, Restaurant restaurant) {
        WorkingHours wh = new WorkingHours();
        wh.setRestaurant(restaurant);
        wh.setDayOfWeek(dto.getDayOfWeek());
        wh.setOpenTime(dto.getOpenTime());
        wh.setCloseTime(dto.getCloseTime());
        return wh;
    }
}