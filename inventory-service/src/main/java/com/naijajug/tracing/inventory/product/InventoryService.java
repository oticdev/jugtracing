package com.naijajug.tracing.inventory.product;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository products;
    private final Tracer tracer;

    public InventoryService(ProductRepository products, Tracer tracer) {
        this.products = products;
        this.tracer = tracer;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return products.findAll();
    }

    @Transactional(readOnly = true)
    public Product findBySku(String sku) {
        return products.findBySku(sku).orElseThrow(() -> new ProductNotFoundException(sku));
    }

    @Transactional
    public Product reserve(String sku, int quantity) {
        Product product = findBySku(sku);
        product.reserve(quantity);
        Span span = tracer.currentSpan();
        if (span != null) {
            span.tag("inventory.sku", sku);
            span.tag("inventory.remaining", product.getQuantityAvailable());
        }
        log.info("Reserved {} x {}, {} left", quantity, sku, product.getQuantityAvailable());
        return product;
    }
}
