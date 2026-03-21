package com.fooddelivery.catalog.service;

import com.fooddelivery.catalog.dto.MenuItemDto;
import com.fooddelivery.catalog.entity.MenuItem;
import com.fooddelivery.catalog.repository.MenuItemRepository;
import com.fooddelivery.catalog.specification.MenuItemSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;

    public MenuItemService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    public MenuItem create(MenuItemDto dto) {
        MenuItem item = new MenuItem();
        item.setCategoryId(dto.getCategoryId());
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setImageUrl(dto.getImageUrl());
        item.setAvailable(dto.isAvailable());
        return menuItemRepository.save(item);
    }

    public MenuItem update(UUID id, MenuItemDto dto) {
        Optional<MenuItem> optional = menuItemRepository.findById(id);
        if (!optional.isPresent()) {
            throw new RuntimeException("MenuItem not found");
        }
        MenuItem item = optional.get();
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrice(dto.getPrice());
        item.setImageUrl(dto.getImageUrl());
        item.setAvailable(dto.isAvailable());
        return menuItemRepository.save(item);
    }

    public MenuItem updateAvailability(UUID id, boolean isAvailable) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MenuItem not found"));
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
}