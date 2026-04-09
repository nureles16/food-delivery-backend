package com.fooddelivery.orders.service;

import com.fooddelivery.catalog.entity.Restaurant;
import com.fooddelivery.orders.dto.Address;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DeliveryFeeService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public BigDecimal calculateDeliveryFee(Restaurant restaurant, Address deliveryAddress, BigDecimal subtotal) {
        if (restaurant.getFreeDeliveryThreshold() != null &&
                subtotal.compareTo(restaurant.getFreeDeliveryThreshold()) >= 0) {
            return BigDecimal.ZERO;
        }

        if (restaurant.getLatitude() == null || restaurant.getLongitude() == null ||
                deliveryAddress.getLat() == null || deliveryAddress.getLng() == null) {
            return restaurant.getDeliveryBaseFee() != null ? restaurant.getDeliveryBaseFee() : BigDecimal.valueOf(50);
        }

        double distance = calculateDistance(
                restaurant.getLatitude(), restaurant.getLongitude(),
                deliveryAddress.getLat(), deliveryAddress.getLng());

        BigDecimal baseFee = restaurant.getDeliveryBaseFee() != null ?
                restaurant.getDeliveryBaseFee() : BigDecimal.valueOf(50);
        BigDecimal perKmFee = restaurant.getDeliveryFeePerKm() != null ?
                restaurant.getDeliveryFeePerKm() : BigDecimal.valueOf(20);

        BigDecimal distanceFee = BigDecimal.valueOf(distance).multiply(perKmFee);
        return baseFee.add(distanceFee).setScale(2, RoundingMode.HALF_UP);
    }
}