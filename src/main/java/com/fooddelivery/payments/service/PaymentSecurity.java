package com.fooddelivery.payments.service;

import com.fooddelivery.auth.security.CustomUserDetails;
import com.fooddelivery.payments.repository.PaymentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("paymentSecurity")
public class PaymentSecurity {

    private final PaymentRepository paymentRepository;

    public PaymentSecurity(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public boolean canAccessPayment(Authentication authentication, UUID paymentId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        boolean isSuperAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (isSuperAdmin) return true;

        return paymentRepository.existsByIdAndUserId(paymentId, user.getId());
    }

    public boolean canAccessMyPayments(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }
}