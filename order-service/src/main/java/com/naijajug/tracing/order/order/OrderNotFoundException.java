package com.naijajug.tracing.order.order;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long id) {
        super("No order with id " + id);
    }
}
