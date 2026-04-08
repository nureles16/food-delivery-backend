package com.fooddelivery.payments.service;

import com.fooddelivery.auth.security.CustomUserDetails;
import com.fooddelivery.payments.dto.*;
import com.fooddelivery.payments.entity.Payment;
import com.fooddelivery.payments.entity.PaymentStatus;
import com.fooddelivery.payments.entity.PayoutStatus;
import com.fooddelivery.payments.repository.PaymentRepository;
import com.fooddelivery.payments.specification.PaymentSpecification;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${freedompay.webhook.secret:test_secret}")
    private String webhookSecret;

    public PaymentService(PaymentRepository paymentRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public InitiatePaymentResponse initiatePayment(CreatePaymentRequest request) {
        log.debug("Initiate payment for orderId={}", request.getOrderId());

        if (request.getClientId() == null) {
            log.error("ClientId is required for payment initiation, orderId={}", request.getOrderId());
            throw new IllegalArgumentException("clientId must be provided by OrderService");
        }

        Optional<Payment> existing = paymentRepository.findByOrderId(request.getOrderId());
        if (existing.isPresent()) {
            log.warn("Payment already exists for orderId={}, returning existing paymentId={}",
                    request.getOrderId(), existing.get().getId());
            String paymentUrl = buildPaymentUrl(existing.get());
            return new InitiatePaymentResponse(existing.get().getId(), paymentUrl);
        }

        if (request.getAmount() == null || request.getDeliveryFee() == null || request.getPlatformFee() == null) {
            throw new IllegalArgumentException("Amount, deliveryFee and platformFee must be non-null");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        BigDecimal subtotal = request.getAmount().subtract(request.getDeliveryFee());
        BigDecimal expectedPlatformFee = subtotal.multiply(BigDecimal.valueOf(0.12))
                .setScale(2, RoundingMode.HALF_UP);
        if (request.getPlatformFee().compareTo(expectedPlatformFee) != 0) {
            log.error("Platform fee mismatch for orderId={}: expected={}, got={}",
                    request.getOrderId(), expectedPlatformFee, request.getPlatformFee());
            throw new IllegalArgumentException(
                    String.format("Platform fee must be exactly 12%% of subtotal (%.2f KGS), but received %.2f KGS",
                            expectedPlatformFee, request.getPlatformFee()));
        }
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setClientId(request.getClientId());
        payment.setRestaurantId(request.getRestaurantId());
        payment.setAmount(request.getAmount());
        payment.setDeliveryFee(request.getDeliveryFee());
        payment.setPlatformFee(request.getPlatformFee());

        BigDecimal restaurantPayout = request.getAmount()
                .subtract(request.getDeliveryFee())
                .subtract(request.getPlatformFee());
        if (restaurantPayout.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Delivery fee + platform fee exceed total amount");
        }
        payment.setRestaurantPayout(restaurantPayout);

        payment.setProvider("FREEDOM_PAY");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setPayoutStatus(PayoutStatus.PENDING);
        payment.setProviderPaymentId(null);
        payment.setProviderResponse("{}");

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created: paymentId={}, orderId={}, clientId={}",
                saved.getId(), saved.getOrderId(), saved.getClientId());

        String paymentUrl = buildPaymentUrl(saved);
        return new InitiatePaymentResponse(saved.getId(), paymentUrl);
    }

    @Transactional
    public void processWebhook(WebhookPayload payload, String receivedHmacSignature) {
        if (!verifyHmacSignature(payload, receivedHmacSignature)) {
            log.error("Invalid HMAC signature for webhook: providerPaymentId={}", payload.getProviderPaymentId());
            throw new SecurityException("Invalid HMAC signature");
        }

        UUID orderId = payload.getOrderId();
        String providerPaymentId = payload.getProviderPaymentId();
        String newStatusFromProvider = payload.getStatus();

        log.info("Processing webhook: orderId={}, providerPaymentId={}, newStatus={}",
                orderId, providerPaymentId, newStatusFromProvider);

        int attempt = 0;
        long delayMs = 100;
        while (attempt < 3) {
            try {
                Payment payment = paymentRepository.findByOrderIdWithLock(orderId)
                        .orElseThrow(() -> new IllegalArgumentException("Payment not found for orderId: " + orderId));

                if (payment.getStatus().isFinal()) {
                    log.warn("Webhook ignored: payment {} already in final status {}. Provider status={}",
                            payment.getId(), payment.getStatus(), newStatusFromProvider);
                    return;
                }

                if (payment.getProviderPaymentId() != null) {
                    if (!payment.getProviderPaymentId().equals(providerPaymentId)) {
                        log.error("ProviderPaymentId mismatch for orderId={}: stored={}, received={}",
                                orderId, payment.getProviderPaymentId(), providerPaymentId);
                        throw new IllegalStateException("ProviderPaymentId already set to different value");
                    }
                    log.debug("Duplicate or updating webhook for providerPaymentId={}, current status={}",
                            providerPaymentId, payment.getStatus());
                } else {
                    payment.setProviderPaymentId(providerPaymentId);
                    payment.setProviderResponse(payload.getRawResponse());
                }

                PaymentStatus newStatus = mapProviderStatus(newStatusFromProvider);
                PaymentStatus currentStatus = payment.getStatus();

                if (!isValidStatusTransition(currentStatus, newStatus)) {
                    log.error("Invalid status transition from {} to {} for paymentId={}, orderId={}",
                            currentStatus, newStatus, payment.getId(), orderId);
                    throw new IllegalStateException(
                            String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
                }

                if (currentStatus != newStatus) {
                    payment.setStatus(newStatus);
                    if (newStatus == PaymentStatus.REFUNDED) {
                        payment.setRefundedAt(LocalDateTime.now());
                    }
                    log.info("Payment status updated: paymentId={}, orderId={}, {} -> {}",
                            payment.getId(), orderId, currentStatus, newStatus);
                }

                paymentRepository.save(payment);

                if (newStatus == PaymentStatus.COMPLETED && currentStatus != PaymentStatus.COMPLETED) {
                    eventPublisher.publishEvent(new PaymentCompletedEvent(payment.getOrderId(), payment.getId()));
                    log.info("Published PAYMENT_COMPLETED event for orderId={}", orderId);
                } else if (newStatus == PaymentStatus.REFUNDED && currentStatus != PaymentStatus.REFUNDED) {
                    eventPublisher.publishEvent(new PaymentRefundedEvent(payment.getOrderId(), payment.getId()));
                    log.info("Published REFUNDED event for orderId={}", orderId);
                } else if (newStatus == PaymentStatus.FAILED && currentStatus != PaymentStatus.FAILED) {
                    eventPublisher.publishEvent(new PaymentFailedEvent(payment.getOrderId(), payment.getId()));
                    log.warn("Payment failed for orderId={}, paymentId={}. Published FAILED event.", orderId, payment.getId());
                }

                return;

            } catch (OptimisticLockException e) {
                attempt++;
                log.warn("Optimistic lock exception for orderId={}, attempt {}/3. Retrying in {} ms...",
                        orderId, attempt, delayMs);
                if (attempt >= 3) {
                    log.error("Failed to process webhook after {} attempts for orderId={}", attempt, orderId);
                    throw new RuntimeException("Concurrent webhook processing failed", e);
                }
                try {
                    Thread.sleep(delayMs);
                    delayMs *= 2;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Webhook processing interrupted", ie);
                }
            }
        }
    }

    @Transactional
    public PaymentResponse refund(UUID paymentId) {
        log.debug("Refund requested for paymentId={}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.warn("Refund called for already refunded paymentId={}", paymentId);
            return mapToResponse(payment);
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            log.error("Refund not allowed for paymentId={} in status={}", paymentId, payment.getStatus());
            throw new IllegalStateException("Refund allowed only for COMPLETED payments");
        }
        if (payment.getPayoutStatus() == PayoutStatus.PAID_OUT) {
            log.error("Cannot refund paymentId={} because payout already done", paymentId);
            throw new IllegalStateException("Cannot refund after payout to restaurant");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        Payment updated = paymentRepository.save(payment);
        log.info("Payment refunded: paymentId={}, orderId={}", updated.getId(), updated.getOrderId());

        eventPublisher.publishEvent(new PaymentRefundedEvent(payment.getOrderId(), payment.getId()));
        return mapToResponse(updated);
    }

    @Transactional
    public PaymentResponse refundByOrderId(UUID orderId) {
        log.info("Auto-refund requested for orderId={}", orderId);
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
        if (paymentOpt.isEmpty()) {
            log.warn("No payment found for orderId={}, cannot auto-refund", orderId);
            return null;
        }
        Payment payment = paymentOpt.get();

        if (payment.getStatus() == PaymentStatus.REFUNDED || payment.getStatus() == PaymentStatus.FAILED) {
            log.info("Payment for orderId={} already in status {}, skipping refund", orderId, payment.getStatus());
            return mapToResponse(payment);
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED && payment.getStatus() != PaymentStatus.PROCESSING) {
            log.info("Payment for orderId={} is not completed (status={}), marking as CANCELLED", orderId, payment.getStatus());
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return mapToResponse(payment);
        }

        return refund(payment.getId());
    }

    @Transactional
    public PaymentResponse markPayoutAsPaid(UUID paymentId) {
        log.debug("Mark payout as paid for paymentId={}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Payout can be marked only for COMPLETED payments");
        }
        if (payment.getPayoutStatus() != PayoutStatus.PENDING) {
            throw new IllegalStateException("Payout already processed for payment " + paymentId);
        }
        payment.setPayoutStatus(PayoutStatus.PAID_OUT);
        payment.setPayoutAt(LocalDateTime.now());
        Payment updated = paymentRepository.save(payment);
        log.info("Payout marked as paid for paymentId={}", updated.getId());
        return mapToResponse(updated);
    }

    public Page<PaymentResponse> getMyPayments(UUID orderId, PaymentStatus status,
                                               BigDecimal minAmount, BigDecimal maxAmount,
                                               LocalDateTime startDate, LocalDateTime endDate,
                                               Pageable pageable) {
        UUID currentClientId = getCurrentClientId();
        Specification<Payment> spec = Specification
                .where(PaymentSpecification.clientIdEquals(currentClientId))
                .and(PaymentSpecification.orderIdEquals(orderId))
                .and(PaymentSpecification.statusEquals(status))
                .and(PaymentSpecification.amountBetween(minAmount, maxAmount))
                .and(PaymentSpecification.createdAtBetween(startDate, endDate));
        return paymentRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

     public Page<PaymentResponse> getPaymentsByOrder(UUID orderId, PaymentStatus status,
                                                    BigDecimal minAmount, BigDecimal maxAmount,
                                                    LocalDateTime startDate, LocalDateTime endDate,
                                                    Pageable pageable) {
        Specification<Payment> spec = Specification
                .where(PaymentSpecification.orderIdEquals(orderId))
                .and(PaymentSpecification.statusEquals(status))
                .and(PaymentSpecification.amountBetween(minAmount, maxAmount))
                .and(PaymentSpecification.createdAtBetween(startDate, endDate));
        return paymentRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    public Page<PaymentResponse> getPayouts(PayoutStatus status, Pageable pageable) {
        Specification<Payment> spec = Specification.where(PaymentSpecification.payoutStatusEquals(status));
        return paymentRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    public Page<PaymentResponse> getCafePayments(UUID restaurantId, PaymentStatus status,
                                                 LocalDateTime startDate, LocalDateTime endDate,
                                                 Pageable pageable) {
        UUID currentUserId = getCurrentUserId();
        UUID usersRestaurantId = getRestaurantIdForCurrentUser();
        if (!usersRestaurantId.equals(restaurantId)) {
            log.error("User {} attempted to access payments of restaurant {}", currentUserId, restaurantId);
            throw new AccessDeniedException("You do not have access to this restaurant's payments");
        }

        Specification<Payment> spec = Specification
                .where(PaymentSpecification.restaurantIdEquals(restaurantId))
                .and(PaymentSpecification.statusEquals(status))
                .and(PaymentSpecification.createdAtBetween(startDate, endDate));
        return paymentRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setOrderId(payment.getOrderId());
        response.setClientId(payment.getClientId());
        response.setAmount(payment.getAmount());
        response.setPlatformFee(payment.getPlatformFee());
        response.setDeliveryFee(payment.getDeliveryFee());
        response.setRestaurantPayout(payment.getRestaurantPayout());
        response.setStatus(payment.getStatus());
        response.setPayoutStatus(payment.getPayoutStatus());
        response.setProviderPaymentId(payment.getProviderPaymentId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }

    private boolean isValidStatusTransition(PaymentStatus current, PaymentStatus next) {
        if (current == next) return true;
        return switch (current) {
            case CREATED -> next == PaymentStatus.PROCESSING
                    || next == PaymentStatus.COMPLETED
                    || next == PaymentStatus.FAILED;
            case PROCESSING -> next == PaymentStatus.COMPLETED || next == PaymentStatus.FAILED;
            case COMPLETED -> next == PaymentStatus.REFUNDED;
            default -> false;
        };
    }

    private PaymentStatus mapProviderStatus(String providerStatus) {
        try {
            return switch (providerStatus.toUpperCase()) {
                case "CREATED" -> PaymentStatus.CREATED;
                case "PROCESSING" -> PaymentStatus.PROCESSING;
                case "COMPLETED" -> PaymentStatus.COMPLETED;
                case "FAILED" -> PaymentStatus.FAILED;
                default -> {
                    log.warn("Unknown provider status '{}', mapping to FAILED", providerStatus);
                    yield PaymentStatus.FAILED;
                }
            };
        } catch (IllegalArgumentException e) {
            log.error("Invalid provider status string '{}', mapping to FAILED", providerStatus);
            return PaymentStatus.FAILED;
        }
    }

    private boolean verifyHmacSignature(WebhookPayload payload, String receivedSignature) {
        try {
            String payloadString = payload.toCanonicalString();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] calculatedSignatureBytes = mac.doFinal(payloadString.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = HexFormat.of().formatHex(calculatedSignatureBytes);
            return calculatedSignature.equals(receivedSignature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC verification failed", e);
            return false;
        }
    }

    private String buildPaymentUrl(Payment payment) {
        return "https://sandbox.freedompay.kg/pay/" + payment.getId();
    }


    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        }
        throw new SecurityException("Unable to extract user ID from security context");
    }

    private UUID getCurrentClientId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            CustomUserDetails user = (CustomUserDetails) principal;
            if (!"CLIENT".equals(user.getRole()) && !"SUPER_ADMIN".equals(user.getRole())) {
                throw new AccessDeniedException("Only clients can access their payments");
            }
            return user.getId();
        }
        throw new SecurityException("Unable to extract client ID from security context");
    }

    private UUID getRestaurantIdForCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            CustomUserDetails user = (CustomUserDetails) principal;
            if (!"CAFE_ADMIN".equals(user.getRole())) {
                throw new AccessDeniedException("Only cafe admins can access restaurant payments");
            }
            UUID cafeId = user.getCafeId();
            if (cafeId == null) {
                throw new IllegalStateException("Cafe admin has no associated restaurant");
            }
            return cafeId;
        }
        throw new SecurityException("Unable to extract restaurant ID from security context");
    }

    public PaymentResponse getPaymentById(UUID paymentId, Authentication authentication) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        String role = user.getRole();
        UUID userId = user.getId();

        boolean isOwner = payment.getClientId().equals(userId);
        boolean isCafeAdmin = "CAFE_ADMIN".equals(role) && payment.getRestaurantId().equals(user.getCafeId());
        boolean isSuperAdmin = "SUPER_ADMIN".equals(role);

        if (!(isOwner || isCafeAdmin || isSuperAdmin)) {
            throw new AccessDeniedException("No access to this payment");
        }

        return mapToResponse(payment);
    }
}