package com.naijajug.tracing.notification;

import java.math.BigDecimal;

/**
 * This service's own view of the event. Deliberately not shared with order-service
 * so the services stay independently deployable.
 */
public record OrderPlacedEvent(Long orderId, String sku, int quantity, String customerEmail,
                               BigDecimal totalNaira, String status, String reason) {
}
