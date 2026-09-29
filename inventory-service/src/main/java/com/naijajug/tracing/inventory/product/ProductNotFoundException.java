package com.naijajug.tracing.inventory.product;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String sku) {
        super("No product with SKU " + sku);
    }
}
