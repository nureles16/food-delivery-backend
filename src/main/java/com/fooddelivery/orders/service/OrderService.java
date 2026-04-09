package com.fooddelivery.orders.service;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.service.AuthService;
import com.fooddelivery.catalog.entity.MenuItem;
import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.catalog.repository.MenuItemRepository;
import com.fooddelivery.catalog.repository.RestaurantRepository;
import com.fooddelivery.catalog.service.RestaurantService;
import com.fooddelivery.exceptions.*;
import com.fooddelivery.mapper.OrderMapper;
import com.fooddelivery.orders.dto.*;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fooddelivery.orders.specification.OrderSpecification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.payments.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantService restaurantService;
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private final AuthService authService;
    private final DeliveryFeeService deliveryFeeService;
    private final OrderMapper orderMapper;

    @Value("${order.auto-cancel.pending-minutes:15}")
    private int autoCancelPendingMinutes;

    @Value("${order.auto-cancel.unconfirmed-paid-minutes:10}")
    private int autoCancelUnconfirmedPaidMinutes;

    private final AtomicLong orderSequence = new AtomicLong(1);

    public OrderService(OrderRepository orderRepository,
                        MenuItemRepository menuItemRepository,
                        RestaurantRepository restaurantRepository,
                        RestaurantService restaurantService,
                        ObjectMapper objectMapper,
                        PaymentService paymentService,
                        AuthService authService,
                        DeliveryFeeService deliveryFeeService,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.restaurantService = restaurantService;
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
        this.authService = authService;
        this.deliveryFeeService = deliveryFeeService;
        this.orderMapper = orderMapper;
    }

    private User getCurrentUser() { return authService.getCurrentUser(); }
    private Order findOrderForUpdate(UUID orderId) {
        return orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
    }
    private String generateOrderNumber() {
        Long seq;
        try {
            seq = orderRepository.getNextOrderNumber();
        } catch (Exception e) {
            seq = orderSequence.getAndIncrement();
            log.warn("Using fallback in-memory sequence for order number generation");
        }
        int year = Year.now().getValue();
        return String.format("#BK-%d-%05d", year, seq);
    }
    private void validateClientOwnership(UUID orderId, UUID clientId) {
        Order order = findOrder(orderId);
        if (!order.getClientId().equals(clientId)) {
            throw new ForbiddenException("Order does not belong to this client");
        }
    }
    private void validateCafeOwnership(UUID orderId, UUID cafeId) {
        Order order = findOrder(orderId);
        if (!order.getRestaurantId().equals(cafeId)) {
            throw new ForbiddenException("Order does not belong to this cafe");
        }
        Restaurant restaurant = restaurantRepository.findById(cafeId)
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));
        if (!restaurant.isActive() || !restaurant.isVerified()) {
            throw new ForbiddenException("Restaurant is deactivated or not verified. Cannot process orders.");
        }
    }
    private String extractDeliveryStatus(OrderStatus status) {
        switch (status) {
            case COURIER_ASSIGNED: return "COURIER_ASSIGNED";
            case DELIVERING: return "DELIVERING";
            case DELIVERED: return "DELIVERED";
            default: return "NOT_YET_ASSIGNED";
        }
    }
    private BigDecimal calculateDeliveryFee(Restaurant restaurant, Address address, BigDecimal subtotal) {
        return deliveryFeeService.calculateDeliveryFee(restaurant, address, subtotal);
    }
    private void safeInitiateRefund(Order order) {
        if (paymentService == null) {
            log.error("PaymentService is not available. Refund for order {} must be processed manually.", order.getId());
            return;
        }
        try {
            paymentService.refund(order.getId(), order.getCancelledReason());
            log.info("Refund initiated for order: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to initiate refund for order {}: {}. Order is cancelled but refund pending.", order.getId(), e.getMessage(), e);
        }
    }

    @Transactional
    public OrderResponse createOrder(@Valid CreateOrderRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.CLIENT) {
            throw new ForbiddenException("Only clients can create orders");
        }
        UUID clientId = currentUser.getId();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }

        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new NotFoundException("Restaurant not found"));

        if (!restaurant.isActive() || !restaurant.isVerified() || !restaurant.isOpenNow()) {
            throw new ForbiddenException("Restaurant is not active, not verified, or closed at this time");
        }

        if (!restaurantService.isWithinDeliveryZone(restaurant.getId(), request.getDeliveryAddress())) {
            throw new BadRequestException("Delivery address is outside restaurant's delivery zone");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<Map<String, Object>> snapshotItems = new ArrayList<>();

        for (OrderItemDto itemDto : request.getItems()) {
            MenuItem item = menuItemRepository.findById(itemDto.getItemId())
                    .orElseThrow(() -> new NotFoundException("Menu item not found: " + itemDto.getItemId()));

            if (!item.getRestaurantId().equals(request.getRestaurantId())) {
                throw new BadRequestException("Item " + item.getId() + " does not belong to restaurant " + request.getRestaurantId());
            }

            if (!item.isAvailable()) {
                throw new ConflictException("Item " + item.getName() + " is currently unavailable");
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
            throw new BadRequestException("Order subtotal (" + subtotal + ") is below the minimum order amount of " + minOrderAmount);
        }

        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(snapshotItems);
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to serialize order items", e);
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

        Order saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse assignCourierSystem(UUID orderId, String yandexDeliveryId, String trackingUrl, LocalDateTime estimatedAt) {
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.READY) {
            throw new ConflictException("Order must be READY to assign courier");
        }
        order.setYandexDeliveryId(yandexDeliveryId);
        order.setYandexTrackingUrl(trackingUrl);
        order.setEstimatedDeliveryAt(estimatedAt);
        order.setStatus(OrderStatus.COURIER_ASSIGNED);
        log.info("System assigned courier (Yandex): orderId={}, deliveryId={}", orderId, yandexDeliveryId);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse startDeliveryAutomatically(UUID orderId) {
        Order order = findOrderForUpdate(orderId);
        if (order.getYandexDeliveryId() == null) {
            throw new BadRequestException("Cannot start delivery automatically: no Yandex delivery ID");
        }
        if (order.getStatus() != OrderStatus.COURIER_ASSIGNED) {
            throw new ConflictException("Order must be COURIER_ASSIGNED to start delivery");
        }
        order.setStatus(OrderStatus.DELIVERING);
        log.info("Delivery started automatically (Yandex webhook): orderId={}", orderId);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse completeDeliveryAutomatically(UUID orderId) {
        Order order = findOrderForUpdate(orderId);
        if (order.getYandexDeliveryId() == null) {
            throw new BadRequestException("Cannot complete delivery automatically: no Yandex delivery ID");
        }
        if (order.getStatus() != OrderStatus.DELIVERING) {
            throw new ConflictException("Order must be DELIVERING to mark as delivered");
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        log.info("Delivery completed automatically (Yandex webhook): orderId={}", orderId);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public OrderResponse manualAssignCourier(UUID orderId) {
        User currentUser = getCurrentUser();
        UUID cafeId = currentUser.getCafeId();
        if (cafeId == null) throw new ForbiddenException("Cafe admin has no associated cafe");
        validateCafeOwnership(orderId, cafeId);

        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.READY) {
            throw new ConflictException("Order must be READY to assign courier manually");
        }
        if (order.getYandexDeliveryId() != null) {
            throw new ConflictException("Cannot manually assign courier: order already assigned to Yandex Delivery");
        }
        order.setStatus(OrderStatus.COURIER_ASSIGNED);
        log.info("Manual courier assigned: orderId={}, cafeId={}", orderId, cafeId);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public OrderResponse manualStartDelivery(UUID orderId) {
        User currentUser = getCurrentUser();
        UUID cafeId = currentUser.getCafeId();
        if (cafeId == null) throw new ForbiddenException("Cafe admin has no associated cafe");
        validateCafeOwnership(orderId, cafeId);

        Order order = findOrderForUpdate(orderId);
        if (order.getYandexDeliveryId() != null) {
            throw new BadRequestException("Cannot manually manage order assigned to Yandex Delivery");
        }
        if (order.getStatus() != OrderStatus.COURIER_ASSIGNED) {
            throw new ConflictException("Order must have courier assigned before starting delivery");
        }
        order.setStatus(OrderStatus.DELIVERING);
        log.info("Manual delivery started: orderId={}, cafeId={}", orderId, cafeId);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public OrderResponse manualCompleteDelivery(UUID orderId) {
        User currentUser = getCurrentUser();
        UUID cafeId = currentUser.getCafeId();
        if (cafeId == null) throw new ForbiddenException("Cafe admin has no associated cafe");
        validateCafeOwnership(orderId, cafeId);

        Order order = findOrderForUpdate(orderId);
        if (order.getYandexDeliveryId() != null) {
            throw new BadRequestException("Cannot manually manage order assigned to Yandex Delivery");
        }
        if (order.getStatus() != OrderStatus.DELIVERING) {
            throw new ConflictException("Order must be DELIVERING to mark as delivered");
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        log.info("Manual delivery completed: orderId={}, cafeId={}", orderId, cafeId);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public OrderResponse confirmOrder(UUID orderId) {
        User currentUser = getCurrentUser();
        UUID cafeId = currentUser.getCafeId();
        if (cafeId == null) throw new ForbiddenException("Cafe admin has no associated cafe");
        validateCafeOwnership(orderId, cafeId);

        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ConflictException("Order must be PAID to confirm");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCafeConfirmedAt(LocalDateTime.now());
        log.info("Order confirmed by cafe: orderId={}, cafeId={}", orderId, cafeId);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public OrderResponse markAsCooking(UUID orderId) {
        User currentUser = getCurrentUser();
        UUID cafeId = currentUser.getCafeId();
        if (cafeId == null) throw new ForbiddenException("Cafe admin has no associated cafe");
        validateCafeOwnership(orderId, cafeId);

        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ConflictException("Order must be CONFIRMED before cooking");
        }
        order.setStatus(OrderStatus.COOKING);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public OrderResponse markAsReady(UUID orderId) {
        User currentUser = getCurrentUser();
        UUID cafeId = currentUser.getCafeId();
        if (cafeId == null) throw new ForbiddenException("Cafe admin has no associated cafe");
        validateCafeOwnership(orderId, cafeId);

        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.COOKING) {
            throw new ConflictException("Order must be COOKING before ready, current status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.READY);
        log.info("Order ready: orderId={}, cafeId={}", orderId, cafeId);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public OrderResponse cancelOrderByCafe(UUID orderId, String reason) {
        User currentUser = getCurrentUser();
        UUID cafeId = currentUser.getCafeId();
        if (cafeId == null) throw new ForbiddenException("Cafe admin has no associated cafe");
        validateCafeOwnership(orderId, cafeId);

        Order order = findOrderForUpdate(orderId);
        OrderStatus status = order.getStatus();

        if (status == OrderStatus.COURIER_ASSIGNED ||
                status == OrderStatus.DELIVERING ||
                status == OrderStatus.DELIVERED ||
                status == OrderStatus.REFUNDED) {
            throw new ConflictException("Cafe cannot cancel order in status " + status);
        }

        boolean wasPaid = status != OrderStatus.PENDING && status != OrderStatus.CANCELLED && status != OrderStatus.REFUNDED;

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledReason(reason != null ? reason : "Cancelled by cafe");
        orderRepository.save(order);

        if (wasPaid) {
            safeInitiateRefund(order);
        }
        return orderMapper.toResponse(order);
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public OrderResponse markAsRefunded(UUID orderId) {
        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new ConflictException("Only cancelled orders can be marked as refunded");
        }
        order.setStatus(OrderStatus.REFUNDED);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrderByClient(UUID orderId, String reason) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.CLIENT) {
            throw new ForbiddenException("Only clients can cancel their own orders");
        }
        UUID clientId = currentUser.getId();
        validateClientOwnership(orderId, clientId);

        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new ConflictException("Client can cancel only PENDING or PAID orders");
        }
        boolean wasPaid = order.getStatus() == OrderStatus.PAID;
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledReason(reason != null ? reason : "Cancelled by client");
        orderRepository.save(order);

        if (wasPaid) {
            safeInitiateRefund(order);
        }
        return orderMapper.toResponse(order);
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENT')")
    public OrderResponse markAsPaid(UUID orderId, UUID paymentId, BigDecimal paidAmount) {
        User currentUser = getCurrentUser();
        validateClientOwnership(orderId, currentUser.getId());

        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.PAID && paymentId.equals(order.getPaymentId())) {
            log.info("Duplicate markAsPaid call for order {} with same paymentId {}, ignoring", orderId, paymentId);
            return orderMapper.toResponse(order);
        }

        if (order.getStatus() == OrderStatus.PAID && !paymentId.equals(order.getPaymentId())) {
            throw new ConflictException(
                    String.format("Order %s is already paid with different paymentId %s", orderId, order.getPaymentId())
            );
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Order cannot be paid because it is not in PENDING state");
        }

        if (paidAmount == null || paidAmount.compareTo(order.getTotalAmount()) != 0) {
            throw new BadRequestException(
                    String.format("Payment amount %s does not match order total %s", paidAmount, order.getTotalAmount())
            );
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentId(paymentId);
        order.setPaidAt(LocalDateTime.now());
        log.info("Order marked as PAID: orderId={}, paymentId={}, amount={}", orderId, paymentId, paidAmount);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    @PreAuthorize("hasRole('CLIENT')")
    public OrderResponse markPaymentFailed(UUID orderId, String reason) {
        User currentUser = getCurrentUser();
        validateClientOwnership(orderId, currentUser.getId());

        Order order = findOrderForUpdate(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Cannot mark payment failed: order is not in PENDING state");
        }
        order.setStatus(OrderStatus.CANCELLED);
        String cancelReason = reason != null ? reason : "Payment failed";
        order.setCancelledReason(cancelReason);
        log.info("Payment failed for order: orderId={}, reason={}", orderId, cancelReason);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Scheduled(fixedDelay = 60000)
    public void autoCancelExpiredOrders() { /* без изменений */ }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void autoCancelPendingOrders() { /* без изменений */ }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void autoCancelUnconfirmedPaidOrders() { /* без изменений */ }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('CAFE_ADMIN')")
    public Page<OrderResponse> getRestaurantOrders(OrderStatus status,
                                                   String orderNumber,
                                                   LocalDateTime startDate,
                                                   LocalDateTime endDate,
                                                   BigDecimal minAmount,
                                                   BigDecimal maxAmount,
                                                   Pageable pageable) {
        User currentUser = getCurrentUser();
        UUID restaurantId = currentUser.getCafeId();
        if (restaurantId == null) {
            throw new ForbiddenException("Cafe admin has no associated restaurant");
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate cannot be after endDate");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BadRequestException("minAmount cannot be greater than maxAmount");
        }

        Specification<Order> spec = Specification
                .where(OrderSpecification.restaurantIdEquals(restaurantId))
                .and(OrderSpecification.statusEquals(status))
                .and(OrderSpecification.orderNumberContains(orderNumber))
                .and(OrderSpecification.createdAtBetween(startDate, endDate))
                .and(OrderSpecification.totalAmountBetween(minAmount, maxAmount));
        return orderRepository.findAll(spec, pageable).map(orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getClientOrders(OrderStatus status,
                                               String orderNumber,
                                               LocalDateTime startDate,
                                               LocalDateTime endDate,
                                               BigDecimal minAmount,
                                               BigDecimal maxAmount,
                                               Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.CLIENT) {
            throw new ForbiddenException("Only clients can view their own orders");
        }
        UUID clientId = currentUser.getId();

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate cannot be after endDate");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BadRequestException("minAmount cannot be greater than maxAmount");
        }

        Specification<Order> spec = Specification
                .where(OrderSpecification.clientIdEquals(clientId))
                .and(OrderSpecification.statusEquals(status))
                .and(OrderSpecification.orderNumberContains(orderNumber))
                .and(OrderSpecification.createdAtBetween(startDate, endDate))
                .and(OrderSpecification.totalAmountBetween(minAmount, maxAmount));
        return orderRepository.findAll(spec, pageable).map(orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        User currentUser = getCurrentUser();
        Order order = findOrder(orderId);

        switch (currentUser.getRole()) {
            case CLIENT:
                if (!order.getClientId().equals(currentUser.getId())) {
                    throw new ForbiddenException("Access denied");
                }
                break;
            case CAFE_ADMIN:
                UUID cafeId = currentUser.getCafeId();
                if (cafeId == null || !order.getRestaurantId().equals(cafeId)) {
                    throw new ForbiddenException("Access denied");
                }
                break;
            case SUPER_ADMIN:
                break;
            default:
                throw new ForbiddenException("Invalid role");
        }
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public TrackingInfoDto getTrackingInfo(UUID orderId) {
        User currentUser = getCurrentUser();
        Order order = findOrder(orderId);
        boolean authorized = false;

        switch (currentUser.getRole()) {
            case CLIENT:
                if (order.getClientId().equals(currentUser.getId())) {
                    authorized = true;
                }
                break;
            case CAFE_ADMIN:
                UUID cafeId = currentUser.getCafeId();
                if (cafeId != null && order.getRestaurantId().equals(cafeId)) {
                    authorized = true;
                }
                break;
            case SUPER_ADMIN:
                authorized = true;
                break;
            default:
                authorized = false;
        }

        if (!authorized) {
            throw new ForbiddenException("Access denied to tracking info");
        }

        String deliveryStatus = extractDeliveryStatus(order.getStatus());
        String trackingUrl = order.getYandexTrackingUrl();
        String estimatedDeliveryAt = order.getEstimatedDeliveryAt() != null
                ? order.getEstimatedDeliveryAt().toString()
                : null;
        return new TrackingInfoDto(deliveryStatus, trackingUrl, estimatedDeliveryAt);
    }
}