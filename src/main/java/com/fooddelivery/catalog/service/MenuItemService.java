package com.fooddelivery.catalog.service;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.service.AuthService;
import com.fooddelivery.catalog.dto.MenuItemDto;
import com.fooddelivery.catalog.entity.MenuCategory;
import com.fooddelivery.catalog.entity.MenuItem;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.repository.MenuCategoryRepository;
import com.fooddelivery.catalog.repository.MenuItemRepository;
import com.fooddelivery.catalog.repository.RestaurantRepository;
import com.fooddelivery.catalog.specification.MenuItemSpecification;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class MenuItemService {

    private static final Logger log = LoggerFactory.getLogger(MenuItemService.class);

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final AuthService authService;
    private final MenuCategoryRepository menuCategoryRepository;

    public MenuItemService(MenuItemRepository menuItemRepository,
                           RestaurantRepository restaurantRepository,
                           AuthService authService, MenuCategoryRepository menuCategoryRepository) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.authService = authService;
        this.menuCategoryRepository = menuCategoryRepository;
    }

    @Transactional
    public MenuItem create(MenuItemDto dto) {
        User currentUser = authService.getCurrentUser();

        MenuCategory category = menuCategoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + dto.getCategoryId()));

        Restaurant restaurant = restaurantRepository.findById(category.getRestaurant())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found for category"));

        checkRestaurantAccess(currentUser, restaurant.getId());

        MenuItem item = new MenuItem();
        item.setCategoryId(category.getId()); // используем ID категории
        // заполняем остальные поля из dto
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setImageUrl(dto.getImageUrl());
        item.setAvailable(dto.isAvailable());
        item.setWeightGrams(dto.getWeightGrams());
        item.setAllergens(dto.getAllergens());
        item.setTags(dto.getTags());

        return menuItemRepository.save(item);
    }

    @Transactional
    public MenuItem update(UUID id, MenuItemDto dto) {
        User currentUser = authService.getCurrentUser();

        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MenuItem not found: " + id));

        UUID newCategoryId = dto.getCategoryId();
        if (newCategoryId != null && !newCategoryId.equals(item.getCategoryId())) {
            MenuCategory newCategory = menuCategoryRepository.findById(newCategoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + newCategoryId));
            Restaurant restaurant = restaurantRepository.findById(newCategory.getRestaurant())
                    .orElseThrow(() -> new EntityNotFoundException("Restaurant not found for category"));
            checkRestaurantAccess(currentUser, restaurant.getId());
            item.setCategoryId(newCategoryId);
        } else {
            MenuCategory existingCategory = menuCategoryRepository.findById(item.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + item.getCategoryId()));
            Restaurant restaurant = restaurantRepository.findById(existingCategory.getRestaurant())
                    .orElseThrow(() -> new EntityNotFoundException("Restaurant not found for category"));
            checkRestaurantAccess(currentUser, restaurant.getId());
        }

        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setImageUrl(dto.getImageUrl());
        item.setAvailable(dto.isAvailable());
        item.setWeightGrams(dto.getWeightGrams());
        item.setAllergens(dto.getAllergens());
        item.setTags(dto.getTags());

        return menuItemRepository.save(item);
    }

    @Transactional
    public MenuItem updateAvailability(UUID id, boolean isAvailable) {
        User currentUser = authService.getCurrentUser();

        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MenuItem not found: " + id));

        MenuCategory category = menuCategoryRepository.findById(item.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found for menu item"));

        Restaurant restaurant = restaurantRepository.findById(category.getRestaurant())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found for category"));

        checkRestaurantAccess(currentUser, restaurant.getId());

        item.setAvailable(isAvailable);
        return menuItemRepository.save(item);
    }

    public Page<MenuItem> getByCategory(UUID categoryId,
                                        String name,
                                        BigDecimal minPrice,
                                        BigDecimal maxPrice,
                                        Boolean available,
                                        Pageable pageable) {

        Specification<MenuItem> spec = Specification
                .where(MenuItemSpecification.categoryIdEquals(categoryId))
                .and(MenuItemSpecification.nameContains(name))
                .and(MenuItemSpecification.priceBetween(minPrice, maxPrice))
                .and(MenuItemSpecification.availableEquals(available));

        return menuItemRepository.findAll(spec, pageable);
    }

    @NotNull
    private MenuItem getMenuItem(MenuItemDto dto, MenuItem item) {
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setImageUrl(dto.getImageUrl());
        item.setAvailable(dto.isAvailable());
        item.setWeightGrams(dto.getWeightGrams());
        item.setAllergens(dto.getAllergens());
        item.setTags(dto.getTags());
        return menuItemRepository.save(item);
    }

    private void checkRestaurantAccess(User user, UUID restaurantId) {
        if (user.getRole() == Role.SUPER_ADMIN) {
            return;
        }
        if (user.getRole() == Role.CAFE_ADMIN) {
            if (user.getCafeId() == null) {
                log.warn("Access denied: CAFE_ADMIN user {} has no associated cafe", user.getId());
                throw new AccessDeniedException("You are not assigned to any restaurant");
            }
            if (!user.getCafeId().equals(restaurantId)) {
                log.warn("Access denied: user {} tried to modify menu of restaurant {} but belongs to {}",
                        user.getId(), restaurantId, user.getCafeId());
                throw new AccessDeniedException("You don't have permission to modify this restaurant's menu");
            }
            return;
        }
        log.warn("Access denied: user {} with role {} tried to modify menu", user.getId(), user.getRole());
        throw new AccessDeniedException("Only SUPER_ADMIN or CAFE_ADMIN can modify menu");
    }

    @Transactional
    public void deleteMenuItem(UUID id) {
        User currentUser = authService.getCurrentUser();

        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MenuItem not found: " + id));

        MenuCategory category = menuCategoryRepository.findById(item.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found for menu item"));

        Restaurant restaurant = restaurantRepository.findById(category.getRestaurant()) // исправленное поле
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found for category"));

        checkRestaurantAccess(currentUser, restaurant.getId());

        menuItemRepository.delete(item);
        log.info("MenuItem {} deleted by user {}", id, currentUser.getId());
    }
}