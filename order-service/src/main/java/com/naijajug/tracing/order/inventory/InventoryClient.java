package com.naijajug.tracing.order.inventory;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api/inventory")
public interface InventoryClient {

    @GetExchange("/{sku}")
    StockResponse getStock(@PathVariable String sku);

    @PostExchange("/reserve")
    StockResponse reserve(@RequestBody ReserveRequest request);
}
