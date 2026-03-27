package com.fooddelivery.orders.service;

import com.fooddelivery.catalog.entity.MenuItem;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.repository.MenuItemRepository;
import com.fooddelivery.catalog.repository.RestaurantRepository;
import com.fooddelivery.catalog.service.RestaurantService;
import com.fooddelivery.orders.dto.CreateOrderRequest;
import com.fooddelivery.orders.dto.OrderItemDto;
import com.fooddelivery.orders.dto.TrackingInfoDto;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fooddelivery.orders.specification.OrderSpecification;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantService restaurantService;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        MenuItemRepository menuItemRepository,
                        RestaurantRepository restaurantRepository,
                        RestaurantService restaurantService,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantService = restaurantService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request, UUID clientId) {
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new OrderException("Restaurant not found"));

        if (!restaurant.isActive() || !restaurant.isOpenNow()) {
            throw new OrderException("Restaurant is not active or closed at this time");
        }

        if (!restaurantService.isWithinDeliveryZone(restaurant.getId(), request.getDeliveryAddress())) {
            throw new OrderException("Delivery address is outside restaurant's delivery zone");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<Map<String, Object>> snapshotItems = new ArrayList<>();

        for (OrderItemDto itemDto : request.getItems()) {
            MenuItem item = menuItemRepository.findById(itemDto.getItemId())
                    .orElseThrow(() -> new OrderException("Menu item not found: " + itemDto.getItemId()));

            if (!item.getRestaurantId().equals(request.getRestaurantId())) {
                throw new OrderException("Item " + item.getId() + " does not belong to restaurant " + request.getRestaurantId());
            }

            if (!item.isAvailable()) {
                throw new OrderException("Item " + item.getName() + " is currently unavailable");
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

        BigDecimal minOrderAmount = restaurant.getMinOrderAmount() != null ? restaurant.getMinOrderAmount() : BigDecimal.ZERO;
        if (subtotal.compareTo(minOrderAmount) < 0) {
            throw new OrderException("Order subtotal (" + subtotal + ") is below the minimum order amount of " + minOrderAmount);
        }

        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(snapshotItems);
        } catch (Exception e) {
            throw new OrderException("Failed to serialize order items");
        }

        BigDecimal commissionRate = restaurant.getCommissionRate() != null
                ? restaurant.getCommissionRate()
                : BigDecimal.valueOf(12.00);
        BigDecimal platformCommission = subtotal.multiply(commissionRate.divide(BigDecimal.valueOf(100)));

        BigDecimal deliveryFee = BigDecimal.valueOf(100);

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setClientId(clientId);
        order.setRestaurantId(request.getRestaurantId());
        order.setStatus(OrderStatus.PENDING);
        order.setItems(itemsJson);
        order.setSubtotal(subtotal);
        order.setPlatformCommission(platformCommission);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(subtotal.add(deliveryFee));
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Transactional
    public Order markAsPaid(UUID orderId, UUID paymentId) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Order cannot be paid because it is not in PENDING state");
        }
        order.setStatus(OrderStatus.PAID);
        order.setPaymentId(paymentId);
        order.setPaidAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmOrder(UUID orderId, UUID cafeId) {
        validateCafeOwnership(orderId, cafeId);
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new InvalidOrderStateException("Order must be PAID to confirm");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCafeConfirmedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional
    public Order markAsCooking(UUID orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException("Order must be CONFIRMED before cooking");
        }
        order.setStatus(OrderStatus.COOKING);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markAsReady(UUID orderId, UUID cafeId) {
        Order order = findOrder(orderId);
        if (!order.getRestaurantId().equals(cafeId)) {
            throw new AccessDeniedException("You can only manage orders of your own restaurant");
        }
        if (order.getStatus() != OrderStatus.COOKING) {
            throw new InvalidOrderStateException("Order must be COOKING before ready, current status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.READY);
        return orderRepository.save(order);
    }

    @Transactional
    public Order assignCourier(UUID orderId, String yandexDeliveryId, String trackingUrl, LocalDateTime estimatedAt) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.READY) {
            throw new InvalidOrderStateException("Order must be READY to assign courier");
        }
        order.setYandexDeliveryId(yandexDeliveryId);
        order.setYandexTrackingUrl(trackingUrl);
        order.setEstimatedDeliveryAt(estimatedAt);
        order.setStatus(OrderStatus.COURIER_ASSIGNED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markAsDelivering(UUID orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.COURIER_ASSIGNED) {
            throw new InvalidOrderStateException("Order must have courier assigned before delivering");
        }
        order.setStatus(OrderStatus.DELIVERING);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markAsDelivered(UUID orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.DELIVERING) {
            throw new InvalidOrderStateException("Order must be DELIVERING to mark as delivered");
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(UUID orderId, boolean isAdminOrCafe, String reason) {
        Order order = findOrder(orderId);
        boolean cancellable;

        if (isAdminOrCafe) {
            cancellable = order.getStatus() != OrderStatus.DELIVERING &&
                    order.getStatus() != OrderStatus.DELIVERED &&
                    order.getStatus() != OrderStatus.REFUNDED;
        } else {
            cancellable = order.getStatus() == OrderStatus.PENDING;
        }

        if (!cancellable) {
            throw new InvalidOrderStateException("Order cannot be cancelled in status " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledReason(reason);
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrderByClient(UUID orderId, UUID clientId, String reason) {
        validateClientOwnership(orderId, clientId);
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Client can cancel only PENDING orders");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledReason(reason != null ? reason : "Cancelled by client");
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrderByCafe(UUID orderId, UUID cafeId, String reason) {
        validateCafeOwnership(orderId, cafeId);
        Order order = findOrder(orderId);
        OrderStatus status = order.getStatus();

        if (status == OrderStatus.COURIER_ASSIGNED ||
                status == OrderStatus.DELIVERING ||
                status == OrderStatus.DELIVERED ||
                status == OrderStatus.REFUNDED) {
            throw new InvalidOrderStateException("Cafe cannot cancel order in status " + status);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledReason(reason);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markAsRefunded(UUID orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Only cancelled orders can be marked as refunded");
        }
        order.setStatus(OrderStatus.REFUNDED);
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

    public Order getOrderById(UUID orderId, UUID userId, String role, UUID cafeId) {
        Order order = findOrder(orderId);
        if ("CLIENT".equals(role)) {
            if (!order.getClientId().equals(userId)) {
                throw new AccessDeniedException("Access denied");
            }
        } else if ("CAFE_ADMIN".equals(role)) {
            if (cafeId == null || !order.getRestaurantId().equals(cafeId)) {
                throw new AccessDeniedException("Access denied");
            }
        } else if ("SUPER_ADMIN".equals(role)) {
        } else {
            throw new AccessDeniedException("Invalid role");
        }
        return order;
    }

    public TrackingInfoDto getTrackingInfo(UUID orderId, UUID clientId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getClientId().equals(clientId)) {
            throw new OrderException("Access denied: order does not belong to this client");
        }

        String deliveryStatus = extractDeliveryStatus(order.getStatus());
        String trackingUrl = order.getYandexTrackingUrl();
        String estimatedDeliveryAt = order.getEstimatedDeliveryAt() != null
                ? order.getEstimatedDeliveryAt().toString()
                : null;

        return new TrackingInfoDto(deliveryStatus, trackingUrl, estimatedDeliveryAt);
    }

    private String extractDeliveryStatus(OrderStatus status) {
        switch (status) {
            case COURIER_ASSIGNED:
                return "COURIER_ASSIGNED";
            case DELIVERING:
                return "DELIVERING";
            case DELIVERED:
                return "DELIVERED";
            default:
                return "NOT_YET_ASSIGNED";
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void autoCancelExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime pendingThreshold = now.minusMinutes(15);
        List<Order> pendingOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING, pendingThreshold);
        for (Order order : pendingOrders) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledReason("Auto-cancelled: payment not completed within 15 minutes");
            orderRepository.save(order);
        }

        LocalDateTime paidThreshold = now.minusMinutes(10);
        List<Order> paidOrders = orderRepository.findByStatusAndPaidAtBefore(
                OrderStatus.PAID, paidThreshold);
        for (Order order : paidOrders) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledReason("Auto-cancelled: restaurant did not confirm within 10 minutes after payment");
            orderRepository.save(order);
        }
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private String generateOrderNumber() {
        Long seq = orderRepository.getNextOrderNumber();
        int year = Year.now().getValue();
        return String.format("#BK-%d-%05d", year, seq);
    }

    public static class OrderException extends RuntimeException {
        public OrderException(String message) { super(message); }
    }

    public static class OrderNotFoundException extends OrderException {
        public OrderNotFoundException(UUID id) { super("Order not found: " + id); }
    }

    public static class InvalidOrderStateException extends OrderException {
        public InvalidOrderStateException(String message) { super(message); }
    }

    private void validateClientOwnership(UUID orderId, UUID clientId) {
        Order order = findOrder(orderId);
        if (!order.getClientId().equals(clientId)) {
            throw new AccessDeniedException("Order does not belong to this client");
        }
    }
    private void validateCafeOwnership(UUID orderId, UUID cafeId) {
        Order order = findOrder(orderId);
        if (!order.getRestaurantId().equals(cafeId)) {
            throw new AccessDeniedException("Order does not belong to this cafe");
        }
    }
}