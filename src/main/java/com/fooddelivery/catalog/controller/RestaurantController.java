package com.fooddelivery.catalog.controller;

import com.fooddelivery.catalog.dto.RestaurantRequest;
import com.fooddelivery.catalog.dto.RestaurantResponseDTO;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.service.RestaurantService;
import com.fooddelivery.mapper.RestaurantMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Restaurants", description = "API для управления ресторанами")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final RestaurantMapper restaurantMapper;

    public RestaurantController(RestaurantService restaurantService, RestaurantMapper restaurantMapper) {
        this.restaurantService = restaurantService;
        this.restaurantMapper = restaurantMapper;
    }

    @Operation(
            summary = "Получить список ресторанов",
            description = "Публичный endpoint. Возвращает список активных ресторанов"
    )
    @ApiResponse(responseCode = "200", description = "Список ресторанов успешно получен")
    @GetMapping("/restaurants")
    public Page<RestaurantResponseDTO> getRestaurants(
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String cuisineType,
            @RequestParam(required = false) BigDecimal minOrderAmount,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean openNow) {

        // Сервис возвращает Page<Restaurant>, затем маппим
        Page<Restaurant> restaurantPage = restaurantService.getAllRestaurants(
                pageable, name, city, cuisineType, minOrderAmount, true, openNow);

        return restaurantPage.map(restaurantMapper::toResponseDTO);
    }

    @Operation(
            summary = "Создать ресторан",
            description = "Создание ресторана (только SUPER_ADMIN)"
    )
    @ApiResponse(responseCode = "200", description = "Ресторан успешно создан")
    @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    @PostMapping("/admin/restaurants")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public RestaurantResponseDTO createRestaurant(@Valid @RequestBody RestaurantRequest request) {
        Restaurant restaurant = restaurantService.createRestaurant(request);
        return restaurantMapper.toResponseDTO(restaurant);
    }

    @Operation(
            summary = "Обновить ресторан",
            description = "Обновление профиля ресторана (CAFE_ADMIN)"
    )
    @ApiResponse(responseCode = "200", description = "Ресторан успешно обновлен")
    @ApiResponse(responseCode = "404", description = "Ресторан не найден")
    @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    @PatchMapping("/cafe/profile/{id}")
    @PreAuthorize("hasAuthority('CAFE_ADMIN') or hasAuthority('SUPER_ADMIN')")
    public RestaurantResponseDTO updateRestaurant(@PathVariable UUID id, @Valid @RequestBody RestaurantRequest request) {
        Restaurant restaurant = restaurantService.updateRestaurant(id, request);
        return restaurantMapper.toResponseDTO(restaurant);
    }

    @Operation(summary = "Получить ресторан по slug", description = "Публичный endpoint")
    @ApiResponse(responseCode = "200", description = "Ресторан найден")
    @ApiResponse(responseCode = "404", description = "Ресторан не найден")
    @GetMapping("/restaurants/{slug}")
    public RestaurantResponseDTO getRestaurantBySlug(@PathVariable String slug) {
        Restaurant restaurant = restaurantService.getRestaurantBySlug(slug);
        return restaurantMapper.toResponseDTO(restaurant);
    }
}