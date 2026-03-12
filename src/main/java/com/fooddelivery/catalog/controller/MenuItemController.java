package com.fooddelivery.catalog.controller;

import com.fooddelivery.catalog.dto.MenuItemDto;
import com.fooddelivery.catalog.entity.MenuItem;
import com.fooddelivery.catalog.service.MenuItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cafe/menu/items")
@Tag(name = "Menu Items", description = "CRUD операции для позиций меню")
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @Operation(summary = "Создать позицию меню", description = "Создаёт новую позицию меню в указанной категории")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Позиция успешно создана"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<MenuItem> create(@RequestBody MenuItemDto dto) {
        return ResponseEntity.ok(menuItemService.create(dto));
    }

    @Operation(summary = "Обновить позицию меню", description = "Обновляет существующую позицию меню по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Позиция успешно обновлена"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "404", description = "Позиция не найдена"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<MenuItem> update(@PathVariable UUID id, @RequestBody MenuItemDto dto) {
        return ResponseEntity.ok(menuItemService.update(id, dto));
    }

    @Operation(summary = "Обновить доступность позиции меню", description = "Меняет статус доступности позиции")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статус успешно обновлён"),
            @ApiResponse(responseCode = "404", description = "Позиция не найдена"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAuthority('CAFE_ADMIN')")
    public ResponseEntity<MenuItem> updateAvailability(@PathVariable UUID id,
                                                       @RequestParam boolean isAvailable) {
        return ResponseEntity.ok(menuItemService.updateAvailability(id, isAvailable));
    }

    @Operation(summary = "Получить все позиции по категории", description = "Возвращает список всех позиций меню указанной категории")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список позиций получен"),
            @ApiResponse(responseCode = "404", description = "Категория не найдена")
    })
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<MenuItem>> getByCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(menuItemService.getByCategory(categoryId));
    }
}