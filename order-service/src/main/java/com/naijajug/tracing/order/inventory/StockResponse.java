package com.naijajug.tracing.order.inventory;

import java.math.BigDecimal;

public record StockResponse(String sku, String name, BigDecimal priceNaira, int quantityAvailable) {
}
