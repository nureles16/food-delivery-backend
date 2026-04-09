package com.fooddelivery.catalog.service;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.service.AuthService;
import com.fooddelivery.catalog.dto.RestaurantRequest;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.entity.WorkingHours;
import com.fooddelivery.catalog.repository.RestaurantRepository;
import com.fooddelivery.catalog.repository.WorkingHoursRepository;
import com.fooddelivery.catalog.specification.RestaurantSpecification;
import com.fooddelivery.mapper.RestaurantMapper;
import com.fooddelivery.orders.dto.Address;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);
    private final RestaurantRepository restaurantRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final AuthService authService;
    private final RestaurantMapper restaurantMapper;

    public RestaurantService(RestaurantRepository restaurantRepository,
                             WorkingHoursRepository workingHoursRepository,
                             AuthService authService,
                             RestaurantMapper restaurantMapper) {
        this.restaurantRepository = restaurantRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.authService = authService;
        this.restaurantMapper = restaurantMapper;
    }

    @Transactional
    public Restaurant createRestaurant(RestaurantRequest request) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != Role.SUPER_ADMIN) {
            log.warn("Access denied: user {} tried to create restaurant without SUPER_ADMIN role",
                    currentUser.getId());
            throw new AccessDeniedException("Only SUPER_ADMIN can create restaurants");
        }

        Restaurant restaurant = restaurantMapper.toEntity(request);
        restaurant.setSlug(generateSlug(request.getName()));
        restaurant.setActive(true);
        restaurant.setVerified(false);
        restaurant.setCommissionRate(new BigDecimal("12.00"));

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        if (request.getWorkingHours() != null && !request.getWorkingHours().isEmpty()) {
            List<WorkingHours> workingHoursList = request.getWorkingHours().stream()
                    .map(dto -> restaurantMapper.toWorkingHours(dto, savedRestaurant))
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

        restaurantMapper.updateEntity(restaurant, request);

        if (request.getWorkingHours() != null) {
            workingHoursRepository.deleteByRestaurantId(restaurant.getId());
            List<WorkingHours> newWorkingHours = request.getWorkingHours().stream()
                    .map(dto -> restaurantMapper.toWorkingHours(dto, restaurant))
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
                    return new EntityNotFoundException("Restaurant not found: " + id);
                });
    }

    public Restaurant getRestaurantBySlug(String slug) {
        return restaurantRepository.findBySlug(slug)
                .orElseThrow(() -> {
                    log.error("Restaurant not found by slug: {}, userId={}",
                            slug, getCurrentUserIdSafely());
                    return new EntityNotFoundException("Restaurant not found: " + slug);
                });
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
        if (name == null || name.isBlank()) {
            return "restaurant-" + UUID.randomUUID().toString().substring(0, 8);
        }

        String transliterated = transliterate(name);

        String baseSlug = transliterated
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");

        if (baseSlug.isEmpty()) {
            baseSlug = "restaurant";
        }

        baseSlug = baseSlug.replaceAll("^-+|-+$", "");

        String slug = baseSlug;
        int counter = 1;
        while (restaurantRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }

    private String transliterate(String input) {
        if (input == null) return "";

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{M}");
        String withoutDiacritics = pattern.matcher(normalized).replaceAll("");

        char[] chars = withoutDiacritics.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            switch (c) {
                case 'а': sb.append("a"); break;
                case 'б': sb.append("b"); break;
                case 'в': sb.append("v"); break;
                case 'г': sb.append("g"); break;
                case 'д': sb.append("d"); break;
                case 'е': sb.append("e"); break;
                case 'ё': sb.append("yo"); break;
                case 'ж': sb.append("zh"); break;
                case 'з': sb.append("z"); break;
                case 'и': sb.append("i"); break;
                case 'й': sb.append("y"); break;
                case 'к': sb.append("k"); break;
                case 'л': sb.append("l"); break;
                case 'м': sb.append("m"); break;
                case 'н': sb.append("n"); break;
                case 'о': sb.append("o"); break;
                case 'п': sb.append("p"); break;
                case 'р': sb.append("r"); break;
                case 'с': sb.append("s"); break;
                case 'т': sb.append("t"); break;
                case 'у': sb.append("u"); break;
                case 'ф': sb.append("f"); break;
                case 'х': sb.append("kh"); break;
                case 'ц': sb.append("ts"); break;
                case 'ч': sb.append("ch"); break;
                case 'ш': sb.append("sh"); break;
                case 'щ': sb.append("sch"); break;
                case 'ъ': sb.append(""); break;
                case 'ы': sb.append("y"); break;
                case 'ь': sb.append(""); break;
                case 'э': sb.append("e"); break;
                case 'ю': sb.append("yu"); break;
                case 'я': sb.append("ya"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private String getCurrentUserIdSafely() {
        try {
            return authService.getCurrentUser().getId().toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public boolean isWithinDeliveryZone(UUID restaurantId, Address address) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found: " + restaurantId));

        if (restaurant.getDeliveryZoneRadiusKm() == null) {
            return false;
        }

        if (address.getLat() == null || address.getLng() == null) {
            return false;
        }

        double distance = calculateDistance(
                restaurant.getLatitude(), restaurant.getLongitude(),
                address.getLat(), address.getLng()
        );

        return distance <= restaurant.getDeliveryZoneRadiusKm();
    }
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}