package com.naijajug.tracing.order.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private BigDecimal totalNaira;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private String reason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Order() {
    }

    private Order(String sku, int quantity, String customerEmail, BigDecimal totalNaira,
                  OrderStatus status, String reason) {
        this.sku = sku;
        this.quantity = quantity;
        this.customerEmail = customerEmail;
        this.totalNaira = totalNaira;
        this.status = status;
        this.reason = reason;
    }

    public static Order confirmed(String sku, int quantity, String customerEmail, BigDecimal totalNaira) {
        return new Order(sku, quantity, customerEmail, totalNaira, OrderStatus.CONFIRMED, null);
    }

    public static Order rejected(String sku, int quantity, String customerEmail, String reason) {
        return new Order(sku, quantity, customerEmail, BigDecimal.ZERO, OrderStatus.REJECTED, reason);
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public BigDecimal getTotalNaira() {
        return totalNaira;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
