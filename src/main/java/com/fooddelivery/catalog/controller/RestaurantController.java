package com.fooddelivery.catalog.controller;

import com.fooddelivery.catalog.dto.RestaurantRequest;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Restaurants", description = "API для управления ресторанами")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @Operation(
            summary = "Получить список ресторанов",
            description = "Публичный endpoint. Возвращает список активных ресторанов"
    )
    @ApiResponse(responseCode = "200", description = "Список ресторанов успешно получен")
    @GetMapping("/restaurants")
    public Page<Restaurant> getRestaurants(
            Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String cuisineType,
            @RequestParam(required = false) BigDecimal minOrderAmount,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean openNow) {

        return restaurantService.getAllRestaurants(pageable, name, city, cuisineType, minOrderAmount, true, openNow);
    }

    @Operation(
            summary = "Создать ресторан",
            description = "Создание ресторана (только SUPER_ADMIN)"
    )
    @ApiResponse(responseCode = "200", description = "Ресторан успешно создан")
    @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    @PostMapping("/admin/restaurants")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public Restaurant createRestaurant(
            @Valid @RequestBody RestaurantRequest request) {

        return restaurantService.createRestaurant(request);
    }

    @Operation(
            summary = "Обновить ресторан",
            description = "Обновление профиля ресторана (CAFE_ADMIN)"
    )
    @ApiResponse(responseCode = "200", description = "Ресторан успешно обновлен")
    @ApiResponse(responseCode = "404", description = "Ресторан не найден")
    @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    @PutMapping("/cafe/profile/{id}")
    @PreAuthorize("hasAuthority('CAFE_ADMIN') or hasAuthority('SUPER_ADMIN')")
    public Restaurant updateRestaurant(

            @Parameter(description = "ID ресторана", required = true)
            @PathVariable UUID id,

            @Valid @RequestBody RestaurantRequest request) {

        return restaurantService.updateRestaurant(id, request);
    }

    @GetMapping("/restaurants/{slug}")
    public Restaurant getRestaurantBySlug(@PathVariable String slug) {
        return restaurantService.getRestaurantBySlug(slug);
    }
}