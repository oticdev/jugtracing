package com.naijajug.tracing.order.events;

import com.naijajug.tracing.order.order.Order;

import java.math.BigDecimal;

public record OrderPlacedEvent(Long orderId, String sku, int quantity, String customerEmail,
                               BigDecimal totalNaira, String status, String reason) {

    static OrderPlacedEvent from(Order order) {
        return new OrderPlacedEvent(order.getId(), order.getSku(), order.getQuantity(),
                order.getCustomerEmail(), order.getTotalNaira(), order.getStatus().name(), order.getReason());
    }
}
