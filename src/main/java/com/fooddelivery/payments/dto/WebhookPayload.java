package com.fooddelivery.payments.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"order_id", "payment_id", "status"})
public class WebhookPayload {

    @JsonProperty("payment_id")
    private String providerPaymentId;

    @JsonProperty("order_id")
    private UUID orderId;

    private String status;

    private String rawResponseJson;

    public WebhookPayload() {}

    public WebhookPayload(String providerPaymentId, UUID orderId, String status, String rawResponseJson) {
        this.providerPaymentId = providerPaymentId;
        this.orderId = orderId;
        this.status = status;
        this.rawResponseJson = rawResponseJson;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRawResponse() {
        return rawResponseJson;
    }

    public void setRawResponse(String rawResponseJson) {
        this.rawResponseJson = rawResponseJson;
    }

    public String toCanonicalString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(SerializationFeature.INDENT_OUTPUT);
            Map<String, String> canonicalMap = new LinkedHashMap<>();
            canonicalMap.put("order_id", orderId != null ? orderId.toString() : "");
            canonicalMap.put("payment_id", providerPaymentId != null ? providerPaymentId : "");
            canonicalMap.put("status", status != null ? status : "");
            return mapper.writeValueAsString(canonicalMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize webhook payload", e);
        }
    }
}