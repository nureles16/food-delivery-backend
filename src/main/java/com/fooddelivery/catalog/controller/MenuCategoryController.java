package com.fooddelivery.catalog.controller;

import com.fooddelivery.catalog.dto.CreateCategoryRequest;
import com.fooddelivery.catalog.entity.MenuCategory;
import com.fooddelivery.catalog.service.MenuCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cafe/menu/categories")
@Tag(name = "Menu Category", description = "API для управления меню (только для админов кафе)")
public class MenuCategoryController {

    private final MenuCategoryService categoryService;

    public MenuCategoryController(MenuCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Создать категорию меню", description = "Создаёт новую категорию меню для ресторана. Доступно роли CAFE_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Категория успешно создана"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
            @ApiResponse(responseCode = "404", description = "Ресторан не найден")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public MenuCategory createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @Operation(summary = "Обновить категорию меню", description = "Обновляет существующую категорию меню")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категория успешно обновлена"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
            @ApiResponse(responseCode = "404", description = "Категория не найдена")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public MenuCategory updateCategory(
            @Parameter(description = "ID категории для обновления", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return categoryService.updateCategory(id, request);
    }

    @Operation(summary = "Удалить категорию меню", description = "Удаляет категорию меню. Можно удалить только если нет блюд в категории")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Категория успешно удалена"),
            @ApiResponse(responseCode = "400", description = "Невозможно удалить категорию, так как в ней есть блюда"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
            @ApiResponse(responseCode = "404", description = "Категория не найдена")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public void deleteCategory(
            @Parameter(description = "ID категории для удаления", required = true)
            @PathVariable UUID id
    ) {
        categoryService.deleteCategory(id);
    }

    @Operation(
            summary = "Получить категории ресторана (админский метод)",
            description = "Возвращает категории меню для указанного ресторана с фильтрацией. Доступен только SUPER_ADMIN и CAFE_ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категории успешно получены"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
            @ApiResponse(responseCode = "404", description = "Ресторан не найден")
    })
    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'CAFE_ADMIN')")
    public Page<MenuCategory> getCategoriesByRestaurant(
            @Parameter(description = "ID ресторана", required = true)
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer minPosition,
            @RequestParam(required = false) Integer maxPosition,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return categoryService.getCategoriesByRestaurant(
                restaurantId, name, minPosition, maxPosition, isActive, pageable
        );
    }
}