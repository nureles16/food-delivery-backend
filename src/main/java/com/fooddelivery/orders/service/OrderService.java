package com.fooddelivery.orders.service;

import com.fooddelivery.catalog.entity.MenuItem;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.repository.MenuItemRepository;
import com.fooddelivery.catalog.repository.RestaurantRepository;
import com.fooddelivery.catalog.service.RestaurantService;
import com.fooddelivery.orders.dto.CreateOrderRequest;
import com.fooddelivery.orders.dto.OrderItemDto;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.orders.specification.OrderSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final ObjectMapper objectMapper;
    private final RestaurantRepository restaurantRepository;           // New field
    private final RestaurantService restaurantService;


    public OrderService(OrderRepository orderRepository,
                        MenuItemRepository menuItemRepository,
                        RestaurantRepository restaurantRepository,
                        ObjectMapper objectMapper, RestaurantService restaurantService) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.objectMapper = objectMapper;
        this.restaurantRepository = restaurantRepository;
        this.restaurantService = restaurantService;
    }

    public Order createOrder(CreateOrderRequest request, UUID clientId) {

        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        if (!restaurant.isActive() || !restaurant.isOpenNow()) {
            throw new RuntimeException("Restaurant is not active and closed at this time");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<Map<String, Object>> snapshotItems = new ArrayList<>();

        // 2. Validate items: existence, belong to restaurant, availability
        for (OrderItemDto itemDto : request.getItems()) {

            MenuItem item = menuItemRepository.findById(itemDto.getItemId())
                    .orElseThrow(() -> new RuntimeException("Menu item not found: " + itemDto.getItemId()));

            // FIXED: compare menu item's restaurant ID with request restaurant ID
            if (!item.getRestaurantId().equals(request.getRestaurantId())) {
                throw new RuntimeException("Item " + item.getId() + " does not belong to restaurant " + request.getRestaurantId());
            }

            if (!item.isAvailable()) {
                throw new RuntimeException("Item " + item.getName() + " is currently unavailable");
            }

            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("itemId", item.getId());
            snapshot.put("name", item.getName());
            snapshot.put("price", item.getPrice());
            snapshot.put("quantity", itemDto.getQuantity());

            snapshotItems.add(snapshot);
        }

        // 3. Minimum order amount check
        BigDecimal minOrderAmount = restaurant.getMinOrderAmount() != null ? restaurant.getMinOrderAmount() : BigDecimal.ZERO;
        if (subtotal.compareTo(minOrderAmount) < 0) {
            throw new RuntimeException("Order subtotal (" + subtotal + ") is below the minimum order amount of " + minOrderAmount);
        }

        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(snapshotItems);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize order items");
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setClientId(clientId);
        order.setRestaurantId(request.getRestaurantId());
        order.setStatus(OrderStatus.PENDING);
        order.setItems(itemsJson);
        order.setSubtotal(subtotal);

        BigDecimal deliveryFee = BigDecimal.valueOf(100);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(subtotal.add(deliveryFee));
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    public Page<Order> getClientOrders(UUID clientId,
                                       OrderStatus status,
                                       String orderNumber,
                                       LocalDateTime startDate,
                                       LocalDateTime endDate,
                                       BigDecimal minAmount,
                                       BigDecimal maxAmount,
                                       Pageable pageable) {
        Specification<Order> spec = Specification
                .where(OrderSpecification.clientIdEquals(clientId))
                .and(OrderSpecification.statusEquals(status))
                .and(OrderSpecification.orderNumberContains(orderNumber))
                .and(OrderSpecification.createdAtBetween(startDate, endDate))
                .and(OrderSpecification.totalAmountBetween(minAmount, maxAmount));

        return orderRepository.findAll(spec, pageable);
    }

    public Page<Order> getRestaurantOrders(UUID restaurantId,
                                           OrderStatus status,
                                           String orderNumber,
                                           LocalDateTime startDate,
                                           LocalDateTime endDate,
                                           BigDecimal minAmount,
                                           BigDecimal maxAmount,
                                           Pageable pageable) {
        Specification<Order> spec = Specification
                .where(OrderSpecification.restaurantIdEquals(restaurantId))
                .and(OrderSpecification.statusEquals(status))
                .and(OrderSpecification.orderNumberContains(orderNumber))
                .and(OrderSpecification.createdAtBetween(startDate, endDate))
                .and(OrderSpecification.totalAmountBetween(minAmount, maxAmount));

        return orderRepository.findAll(spec, pageable);
    }

    public Order cancelOrder(UUID orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order cannot be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);

        return orderRepository.save(order);
    }

    public Order confirmOrder(UUID orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order cannot be confirmed");
        }

        order.setStatus(OrderStatus.CONFIRMED);

        return orderRepository.save(order);
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String randomPart = String.valueOf(new Random().nextInt(900) + 100);

        return "ORD-" + datePart + "-" + randomPart;
    }
}