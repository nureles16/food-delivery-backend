package com.fooddelivery.orders.service;

import com.fooddelivery.catalog.entity.MenuItem;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.repository.MenuItemRepository;
import com.fooddelivery.catalog.repository.RestaurantRepository;
import com.fooddelivery.catalog.service.RestaurantService;
import com.fooddelivery.orders.dto.Address;
import com.fooddelivery.orders.dto.CreateOrderRequest;
import com.fooddelivery.orders.dto.OrderItemDto;
import com.fooddelivery.orders.dto.TrackingInfoDto;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fooddelivery.orders.specification.OrderSpecification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.payments.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantService restaurantService;
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository,
                        MenuItemRepository menuItemRepository,
                        RestaurantRepository restaurantRepository,
                        RestaurantService restaurantService,
                        ObjectMapper objectMapper,
                        PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantService = restaurantService;
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
    }

    @Transactional
    public Order createOrder(@Valid CreateOrderRequest request, UUID clientId) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new OrderException("Order must contain at least one item");
        }

        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new OrderException("Restaurant not found"));

        if (!restaurant.isActive() || !restaurant.isVerified() || !restaurant.isOpenNow()) {
            throw new OrderException("Restaurant is not active, not verified, or closed at this time");
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
            throw new OrderException("Failed to serialize order items", e);
        }

        BigDecimal commissionRate = restaurant.getCommissionRate() != null
                ? restaurant.getCommissionRate()
                : BigDecimal.valueOf(12.00);
        BigDecimal platformCommission = subtotal.multiply(commissionRate.divide(BigDecimal.valueOf(100)));

        BigDecimal deliveryFee = calculateDeliveryFee(restaurant, request.getDeliveryAddress(), subtotal);

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

        log.info("Order created: orderNumber={}, clientId={}, restaurantId={}, total={}",
                order.getOrderNumber(), clientId, request.getRestaurantId(), order.getTotalAmount());

        return orderRepository.save(order);
    }

    private BigDecimal calculateDeliveryFee(Restaurant restaurant, Address address, BigDecimal subtotal) {
        log.warn("Using mock delivery fee calculation. Replace with real DeliveryService integration.");
        return BigDecimal.valueOf(100);
    }


    @Transactional
    public Order assignCourierSystem(UUID orderId, String yandexDeliveryId, String trackingUrl, LocalDateTime estimatedAt) {
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.READY) {
            throw new InvalidOrderStateException("Order must be READY to assign courier");
        }
        order.setYandexDeliveryId(yandexDeliveryId);
        order.setYandexTrackingUrl(trackingUrl);
        order.setEstimatedDeliveryAt(estimatedAt);
        order.setStatus(OrderStatus.COURIER_ASSIGNED);
        log.info("System assigned courier (Yandex): orderId={}, deliveryId={}", orderId, yandexDeliveryId);
        return orderRepository.save(order);
    }

    @Transactional
    public Order startDeliveryAutomatically(UUID orderId) {
        Order order = findOrderForUpdate(orderId);
        if (order.getYandexDeliveryId() == null) {
            throw new OrderException("Cannot start delivery automatically: no Yandex delivery ID");
        }
        if (order.getStatus() != OrderStatus.COURIER_ASSIGNED) {
            throw new InvalidOrderStateException("Order must be COURIER_ASSIGNED to start delivery");
        }
        order.setStatus(OrderStatus.DELIVERING);
        log.info("Delivery started automatically (Yandex webhook): orderId={}", orderId);
        return orderRepository.save(order);
    }

    @Transactional
    public Order completeDeliveryAutomatically(UUID orderId) {
        Order order = findOrderForUpdate(orderId);
        if (order.getYandexDeliveryId() == null) {
            throw new OrderException("Cannot complete delivery automatically: no Yandex delivery ID");
        }
        if (order.getStatus() != OrderStatus.DELIVERING) {
            throw new InvalidOrderStateException("Order must be DELIVERING to mark as delivered");
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        log.info("Delivery completed automatically (Yandex webhook): orderId={}", orderId);
        return orderRepository.save(order);
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public Order manualAssignCourier(UUID orderId, UUID cafeId) {
        validateCafeOwnership(orderId, cafeId);
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.READY) {
            throw new InvalidOrderStateException("Order must be READY to assign courier manually");
        }
        if (order.getYandexDeliveryId() != null) {
            throw new OrderException("Cannot manually assign courier: order already assigned to Yandex Delivery");
        }
        order.setStatus(OrderStatus.COURIER_ASSIGNED);
        log.info("Manual courier assigned: orderId={}, cafeId={}", orderId, cafeId);
        return orderRepository.save(order);
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public Order manualStartDelivery(UUID orderId, UUID cafeId) {
        validateCafeOwnership(orderId, cafeId);
        Order order = findOrderForUpdate(orderId);
        if (order.getYandexDeliveryId() != null) {
            throw new OrderException("Cannot manually manage order assigned to Yandex Delivery");
        }
        if (order.getStatus() != OrderStatus.COURIER_ASSIGNED) {
            throw new InvalidOrderStateException("Order must have courier assigned before starting delivery");
        }
        order.setStatus(OrderStatus.DELIVERING);
        log.info("Manual delivery started: orderId={}, cafeId={}", orderId, cafeId);
        return orderRepository.save(order);
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public Order manualCompleteDelivery(UUID orderId, UUID cafeId) {
        validateCafeOwnership(orderId, cafeId);
        Order order = findOrderForUpdate(orderId);
        if (order.getYandexDeliveryId() != null) {
            throw new OrderException("Cannot manually manage order assigned to Yandex Delivery");
        }
        if (order.getStatus() != OrderStatus.DELIVERING) {
            throw new InvalidOrderStateException("Order must be DELIVERING to mark as delivered");
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        log.info("Manual delivery completed: orderId={}, cafeId={}", orderId, cafeId);
        return orderRepository.save(order);
    }


    @Transactional
    public Order markAsPaid(UUID orderId, UUID paymentId, BigDecimal paidAmount) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() == OrderStatus.PAID && paymentId.equals(order.getPaymentId())) {
            log.info("Duplicate markAsPaid call for order {} with same paymentId {}, ignoring", orderId, paymentId);
            return order;
        }

        if (order.getStatus() == OrderStatus.PAID && !paymentId.equals(order.getPaymentId())) {
            throw new InvalidOrderStateException(
                    String.format("Order %s is already paid with different paymentId %s", orderId, order.getPaymentId())
            );
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Order cannot be paid because it is not in PENDING state");
        }

        if (paidAmount == null || paidAmount.compareTo(order.getTotalAmount()) != 0) {
            throw new OrderException(
                    String.format("Payment amount %s does not match order total %s", paidAmount, order.getTotalAmount())
            );
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentId(paymentId);
        order.setPaidAt(LocalDateTime.now());
        log.info("Order marked as PAID: orderId={}, paymentId={}, amount={}", orderId, paymentId, paidAmount);
        return orderRepository.save(order);
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public Order confirmOrder(UUID orderId, UUID cafeId) {
        validateCafeOwnership(orderId, cafeId);
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new InvalidOrderStateException("Order must be PAID to confirm");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCafeConfirmedAt(LocalDateTime.now());
        log.info("Order confirmed by cafe: orderId={}, cafeId={}", orderId, cafeId);
        return orderRepository.save(order);
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public Order markAsCooking(UUID orderId, UUID cafeId) {
        validateCafeOwnership(orderId, cafeId);
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException("Order must be CONFIRMED before cooking");
        }
        order.setStatus(OrderStatus.COOKING);
        return orderRepository.save(order);
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public Order markAsReady(UUID orderId, UUID cafeId) {
        validateCafeOwnership(orderId, cafeId);
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.COOKING) {
            throw new InvalidOrderStateException("Order must be COOKING before ready, current status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.READY);
        log.info("Order ready: orderId={}, cafeId={}", orderId, cafeId);


        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrderByClient(UUID orderId, UUID clientId, String reason) {
        validateClientOwnership(orderId, clientId);
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new InvalidOrderStateException("Client can cancel only PENDING or PAID orders");
        }
        boolean wasPaid = order.getStatus() == OrderStatus.PAID;
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledReason(reason != null ? reason : "Cancelled by client");
        orderRepository.save(order);

        if (wasPaid) {
            safeInitiateRefund(order);
        }
        return order;
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public Order cancelOrderByCafe(UUID orderId, UUID cafeId, String reason) {
        validateCafeOwnership(orderId, cafeId);
        Order order = findOrderForUpdate(orderId);
        OrderStatus status = order.getStatus();

        if (status == OrderStatus.COURIER_ASSIGNED ||
                status == OrderStatus.DELIVERING ||
                status == OrderStatus.DELIVERED ||
                status == OrderStatus.REFUNDED) {
            throw new InvalidOrderStateException("Cafe cannot cancel order in status " + status);
        }

        boolean wasPaid = status != OrderStatus.PENDING && status != OrderStatus.CANCELLED && status != OrderStatus.REFUNDED;

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledReason(reason);
        orderRepository.save(order);

        if (wasPaid) {
            safeInitiateRefund(order);
        }
        return order;
    }

    private void safeInitiateRefund(Order order) {
        try {
            paymentService.refund(order.getId(), order.getCancelledReason());
            log.info("Refund initiated for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to initiate refund for order {}: {}. Order is cancelled but refund pending.", order.getId(), e.getMessage(), e);
        }
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Order markAsRefunded(UUID orderId) {
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Only cancelled orders can be marked as refunded");
        }
        order.setStatus(OrderStatus.REFUNDED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order markPaymentFailed(UUID orderId, String reason) {
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Cannot mark payment failed: order is not in PENDING state");
        }
        order.setStatus(OrderStatus.CANCELLED);
        String cancelReason = reason != null ? reason : "Payment failed";
        order.setCancelledReason(cancelReason);
        log.info("Payment failed for order: orderId={}, reason={}", orderId, cancelReason);
        return orderRepository.save(order);
    }


    @Scheduled(fixedDelay = 60000)
    public void autoCancelExpiredOrders() {
        log.debug("Running auto-cancel job for expired orders");
        autoCancelPendingOrders();
        autoCancelUnconfirmedPaidOrders();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void autoCancelPendingOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        List<Order> pendingOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, threshold);
        for (Order order : pendingOrders) {
            try {
                Order locked = orderRepository.findByIdWithLock(order.getId())
                        .orElseThrow(() -> new OrderNotFoundException(order.getId()));
                if (locked.getStatus() != OrderStatus.PENDING) {
                    continue;
                }
                locked.setStatus(OrderStatus.CANCELLED);
                locked.setCancelledReason("Auto-cancelled: payment not completed within 15 minutes");
                orderRepository.save(locked);
                log.info("Auto-cancelled pending order: {}", order.getId());
            } catch (Exception e) {
                log.error("Failed to auto-cancel pending order {}: {}", order.getId(), e.getMessage(), e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void autoCancelUnconfirmedPaidOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        List<Order> paidOrders = orderRepository.findByStatusAndPaidAtBefore(OrderStatus.PAID, threshold);
        for (Order order : paidOrders) {
            try {
                Order locked = orderRepository.findByIdWithLock(order.getId())
                        .orElseThrow(() -> new OrderNotFoundException(order.getId()));
                if (locked.getStatus() != OrderStatus.PAID) {
                    continue;
                }
                locked.setStatus(OrderStatus.CANCELLED);
                locked.setCancelledReason("Auto-cancelled: restaurant did not confirm within 10 minutes after payment");
                orderRepository.save(locked);
                log.info("Auto-cancelled unpaid confirmed order: {}", order.getId());
                safeInitiateRefund(locked);
            } catch (Exception e) {
                log.error("Failed to auto-cancel paid order {}: {}", order.getId(), e.getMessage(), e);
            }
        }
    }


    @Transactional(readOnly = true)
    public Page<Order> getRestaurantOrders(UUID restaurantId, UUID cafeId,
                                           OrderStatus status,
                                           String orderNumber,
                                           LocalDateTime startDate,
                                           LocalDateTime endDate,
                                           BigDecimal minAmount,
                                           BigDecimal maxAmount,
                                           Pageable pageable) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("minAmount cannot be greater than maxAmount");
        }
        if (cafeId == null || !restaurantId.equals(cafeId)) {
            throw new AccessDeniedException("You can only view orders of your own restaurant");
        }
        Specification<Order> spec = Specification
                .where(OrderSpecification.restaurantIdEquals(restaurantId))
                .and(OrderSpecification.statusEquals(status))
                .and(OrderSpecification.orderNumberContains(orderNumber))
                .and(OrderSpecification.createdAtBetween(startDate, endDate))
                .and(OrderSpecification.totalAmountBetween(minAmount, maxAmount));
        return orderRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> getClientOrders(UUID clientId,
                                       OrderStatus status,
                                       String orderNumber,
                                       LocalDateTime startDate,
                                       LocalDateTime endDate,
                                       BigDecimal minAmount,
                                       BigDecimal maxAmount,
                                       Pageable pageable) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("minAmount cannot be greater than maxAmount");
        }
        Specification<Order> spec = Specification
                .where(OrderSpecification.clientIdEquals(clientId))
                .and(OrderSpecification.statusEquals(status))
                .and(OrderSpecification.orderNumberContains(orderNumber))
                .and(OrderSpecification.createdAtBetween(startDate, endDate))
                .and(OrderSpecification.totalAmountBetween(minAmount, maxAmount));
        return orderRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
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
        } else if (!"SUPER_ADMIN".equals(role)) {
            throw new AccessDeniedException("Invalid role");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public TrackingInfoDto getTrackingInfo(UUID orderId, UUID clientId) {
        return getTrackingInfo(orderId, clientId, "CLIENT", null);
    }

    @Transactional(readOnly = true)
    public TrackingInfoDto getTrackingInfo(UUID orderId, UUID userId, String role, UUID cafeId) {
        Order order = findOrder(orderId);
        boolean authorized = false;
        if ("CLIENT".equals(role) && order.getClientId().equals(userId)) {
            authorized = true;
        } else if ("CAFE_ADMIN".equals(role) && cafeId != null && order.getRestaurantId().equals(cafeId)) {
            authorized = true;
        } else if ("SUPER_ADMIN".equals(role)) {
            authorized = true;
        }
        if (!authorized) {
            throw new AccessDeniedException("Access denied to tracking info");
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
            case COURIER_ASSIGNED: return "COURIER_ASSIGNED";
            case DELIVERING: return "DELIVERING";
            case DELIVERED: return "DELIVERED";
            default: return "NOT_YET_ASSIGNED";
        }
    }

    private Order findOrderForUpdate(UUID orderId) {
        return orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
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
        Restaurant restaurant = restaurantRepository.findById(cafeId)
                .orElseThrow(() -> new OrderException("Restaurant not found"));
        if (!restaurant.isActive() || !restaurant.isVerified()) {
            throw new OrderException("Restaurant is deactivated or not verified. Cannot process orders.");
        }
    }

    public static class OrderException extends RuntimeException {
        public OrderException(String message) { super(message); }
        public OrderException(String message, Throwable cause) { super(message, cause); }
    }

    public static class OrderNotFoundException extends OrderException {
        public OrderNotFoundException(UUID id) { super("Order not found: " + id); }
    }

    public static class InvalidOrderStateException extends OrderException {
        public InvalidOrderStateException(String message) { super(message); }
    }
}