package com.fooddelivery.catalog.service;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.service.AuthService;
import com.fooddelivery.catalog.dto.CategoryWithItemsDto;
import com.fooddelivery.catalog.dto.CreateCategoryRequest;
import com.fooddelivery.catalog.dto.MenuItemDto;
import com.fooddelivery.catalog.entity.MenuCategory;
import com.fooddelivery.catalog.entity.MenuItem;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.repository.MenuCategoryRepository;
import com.fooddelivery.catalog.repository.MenuItemRepository;
import com.fooddelivery.catalog.repository.RestaurantRepository;
import com.fooddelivery.catalog.specification.MenuCategorySpecification;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MenuCategoryService {

    private static final Logger log = LoggerFactory.getLogger(MenuCategoryService.class);

    private final MenuCategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final AuthService authService;

    public MenuCategoryService(MenuCategoryRepository categoryRepository,
                               RestaurantRepository restaurantRepository,
                               MenuItemRepository menuItemRepository,
                               AuthService authService) {
        this.categoryRepository = categoryRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.authService = authService;
    }


    @Transactional
    public MenuCategory createCategory(CreateCategoryRequest request) {
        User currentUser = authService.getCurrentUser();
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("Category name must not be empty");
        }

        UUID restaurantId;
        if (currentUser.getRole() == Role.CAFE_ADMIN) {
            restaurantId = currentUser.getCafeId();
            if (restaurantId == null) {
                throw new IllegalStateException("CAFE_ADMIN has no associated cafe");
            }
        } else if (currentUser.getRole() == Role.SUPER_ADMIN) {
            if (request.getRestaurantId() == null) {
                throw new IllegalArgumentException("restaurantId is required for SUPER_ADMIN");
            }
            restaurantId = request.getRestaurantId();
        } else {
            throw new AccessDeniedException("Only SUPER_ADMIN or CAFE_ADMIN can create categories");
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found: " + restaurantId));

        MenuCategory category = new MenuCategory();
        category.setName(request.getName().trim());
        category.setPosition(request.getPosition() != null ? request.getPosition() : 999);
        category.setActive(request.getActive() != null ? request.getActive() : true);
        category.setRestaurant(restaurant);

        MenuCategory saved = categoryRepository.save(category);
        log.info("Category created: id={}, name={}, restaurantId={}, by user={}",
                saved.getId(), saved.getName(), restaurantId, currentUser.getId());
        return saved;
    }

    @Transactional
    public MenuCategory updateCategory(UUID id, CreateCategoryRequest request) {
        User currentUser = authService.getCurrentUser();
        MenuCategory category = findByIdAndCheckAccess(id, currentUser);

        if (request.getName() != null) {
            if (!StringUtils.hasText(request.getName())) {
                throw new IllegalArgumentException("Category name must not be empty");
            }
            category.setName(request.getName().trim());
        }
        if (request.getPosition() != null) {
            category.setPosition(request.getPosition());
        }
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
        MenuCategory updated = categoryRepository.save(category);
        log.info("Category updated: id={}, name={}, by user={}", updated.getId(), updated.getName(), currentUser.getId());
        return updated;
    }

    @Transactional
    public void deleteCategory(UUID id) {
        User currentUser = authService.getCurrentUser();
        MenuCategory category = findByIdAndCheckAccess(id, currentUser);

        if (menuItemRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("Cannot delete category that contains menu items");
        }
        categoryRepository.delete(category);
        log.info("Category deleted: id={}, name={}, restaurantId={}, by user={}",
                id, category.getName(), category.getRestaurant(), currentUser.getId());
    }

    public Page<MenuCategory> getCategoriesByRestaurant(UUID restaurantId,
                                                        String name,
                                                        Integer minPosition,
                                                        Integer maxPosition,
                                                        Boolean isActive,
                                                        Pageable pageable) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new EntityNotFoundException("Restaurant not found");
        }
        Specification<MenuCategory> spec = Specification
                .where(MenuCategorySpecification.restaurantIdEquals(restaurantId))
                .and(MenuCategorySpecification.nameContains(name))
                .and(MenuCategorySpecification.positionBetween(minPosition, maxPosition))
                .and(isActive != null ? MenuCategorySpecification.isActiveEquals(isActive) : null);
        return categoryRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<CategoryWithItemsDto> getActiveMenuByRestaurantId(UUID restaurantId) {
        List<MenuCategory> activeCategories = categoryRepository.findByRestaurantIdAndIsActiveTrueOrderByPositionAsc(restaurantId);
        if (activeCategories.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> categoryIds = activeCategories.stream().map(MenuCategory::getId).collect(Collectors.toList());
        List<MenuItem> activeItems = menuItemRepository.findByCategoryIdInAndIsAvailableTrue(categoryIds);

        java.util.Map<UUID, List<MenuItemDto>> itemsByCategory = activeItems.stream()
                .collect(Collectors.groupingBy(
                        MenuItem::getCategoryId,
                        Collectors.mapping(this::toMenuItemDto, Collectors.toList())
                ));

        return activeCategories.stream()
                .map(cat -> new CategoryWithItemsDto(
                        cat.getId(),
                        cat.getName(),
                        cat.getPosition(),
                        itemsByCategory.getOrDefault(cat.getId(), Collections.emptyList())
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryWithItemsDto> getActiveMenuByRestaurantSlug(String slug) {
        Restaurant restaurant = restaurantRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found: " + slug));
        return getActiveMenuByRestaurantId(restaurant.getId());
    }


    private MenuCategory findByIdAndCheckAccess(UUID id, User user) {
        MenuCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
        UUID restaurantId = category.getRestaurant();
        checkRestaurantAccess(user, restaurantId);
        return category;
    }

    private void checkRestaurantAccess(User user, UUID restaurantId) {
        if (user.getRole() == Role.SUPER_ADMIN) return;
        if (user.getRole() == Role.CAFE_ADMIN) {
            if (user.getCafeId() == null || !user.getCafeId().equals(restaurantId)) {
                throw new AccessDeniedException("No permission to this restaurant");
            }
            return;
        }
        throw new AccessDeniedException("Only SUPER_ADMIN or CAFE_ADMIN can manage categories");
    }

    private MenuItemDto toMenuItemDto(MenuItem item) {
        return new MenuItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getImageUrl(),
                item.getWeightGrams(),
                item.getAllergens(),
                item.getTags()
        );
    }
}