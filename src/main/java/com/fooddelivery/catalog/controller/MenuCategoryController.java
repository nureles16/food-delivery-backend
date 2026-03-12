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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cafe/menu/categories")
@Tag(name = "Menu Category", description = "API для управления меню")
public class MenuCategoryController {

    private final MenuCategoryService categoryService;

    public MenuCategoryController(MenuCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Создать категорию меню", description = "Создаёт новую категорию меню для ресторана. Доступно роли CAFE_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категория успешно создана"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "404", description = "Ресторан не найден")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public MenuCategory createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @Operation(summary = "Обновить категорию меню", description = "Обновляет существующую категорию меню")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Категория успешно обновлена"),
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
            @ApiResponse(responseCode = "200", description = "Категория успешно удалена"),
            @ApiResponse(responseCode = "404", description = "Категория не найдена"),
            @ApiResponse(responseCode = "400", description = "Невозможно удалить категорию, так как в ней есть блюда")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public void deleteCategory(
            @Parameter(description = "ID категории для удаления", required = true)
            @PathVariable UUID id
    ) {
        categoryService.deleteCategory(id);
    }
}