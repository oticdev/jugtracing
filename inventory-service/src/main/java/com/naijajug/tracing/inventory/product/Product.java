package com.naijajug.tracing.inventory.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal priceNaira;

    @Column(nullable = false)
    private int quantityAvailable;

    @Version
    private long version;

    protected Product() {
    }

    public Product(String sku, String name, BigDecimal priceNaira, int quantityAvailable) {
        this.sku = sku;
        this.name = name;
        this.priceNaira = priceNaira;
        this.quantityAvailable = quantityAvailable;
    }

    public boolean canReserve(int quantity) {
        return quantityAvailable >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new InsufficientStockException(sku, quantity, quantityAvailable);
        }
        quantityAvailable -= quantity;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPriceNaira() {
        return priceNaira;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }
}
