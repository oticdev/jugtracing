package com.naijajug.tracing.inventory.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds demo products on first start only, so restarts don't duplicate rows.
 */
@Component
class InventorySeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InventorySeeder.class);

    private final ProductRepository products;

    InventorySeeder(ProductRepository products) {
        this.products = products;
    }

    @Override
    public void run(String... args) {
        if (products.count() > 0) {
            return;
        }
        products.saveAll(List.of(
                new Product("JOLLOF-001", "Party Jollof Rice", new BigDecimal("3500"), 100_000),
                new Product("SUYA-001", "Beef Suya (1 stick)", new BigDecimal("1500"), 100_000),
                new Product("PUFF-001", "Puff-Puff (10 pcs)", new BigDecimal("1000"), 100_000),
                new Product("CHIN-001", "Chin-Chin (500g)", new BigDecimal("2000"), 100_000),
                // Deliberately tiny, for the out-of-stock demo.
                new Product("ZOBO-001", "Zobo Drink (50cl)", new BigDecimal("800"), 5)));
        log.info("Seeded {} products", products.count());
    }
}
