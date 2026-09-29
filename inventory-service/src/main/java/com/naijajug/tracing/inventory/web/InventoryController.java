package com.naijajug.tracing.inventory.web;

import com.naijajug.tracing.inventory.chaos.ChaosMonkey;
import com.naijajug.tracing.inventory.product.InventoryService;
import com.naijajug.tracing.inventory.product.Product;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
class InventoryController {

    private final InventoryService inventory;
    private final ChaosMonkey chaosMonkey;

    InventoryController(InventoryService inventory, ChaosMonkey chaosMonkey) {
        this.inventory = inventory;
        this.chaosMonkey = chaosMonkey;
    }

    @GetMapping
    List<StockResponse> all() {
        return inventory.findAll().stream().map(StockResponse::from).toList();
    }

    @GetMapping("/{sku}")
    StockResponse stock(@PathVariable String sku) {
        chaosMonkey.maybeMisbehave();
        return StockResponse.from(inventory.findBySku(sku));
    }

    @PostMapping("/reserve")
    StockResponse reserve(@Valid @RequestBody ReserveRequest request) {
        chaosMonkey.maybeMisbehave();
        return StockResponse.from(inventory.reserve(request.sku(), request.quantity()));
    }

    record ReserveRequest(@NotBlank String sku, @Min(1) int quantity) {
    }

    record StockResponse(String sku, String name, BigDecimal priceNaira, int quantityAvailable) {

        static StockResponse from(Product product) {
            return new StockResponse(product.getSku(), product.getName(),
                    product.getPriceNaira(), product.getQuantityAvailable());
        }
    }
}
