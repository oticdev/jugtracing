package com.naijajug.tracing.inventory.product;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String sku, int requested, int available) {
        super("Cannot reserve %d x %s, only %d left".formatted(requested, sku, available));
    }
}
