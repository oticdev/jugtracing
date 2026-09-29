package com.naijajug.tracing.order.web;

import com.naijajug.tracing.order.order.Order;
import com.naijajug.tracing.order.order.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService orders;

    OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse place(@Valid @RequestBody PlaceOrderRequest request) {
        return OrderResponse.from(orders.placeOrder(request.sku(), request.quantity(), request.customerEmail()));
    }

    @GetMapping("/{id}")
    OrderResponse get(@PathVariable Long id) {
        return OrderResponse.from(orders.findById(id));
    }

    @GetMapping
    List<OrderResponse> all() {
        return orders.findAll().stream().map(OrderResponse::from).toList();
    }

    record PlaceOrderRequest(@NotBlank String sku, @Min(1) int quantity, @NotBlank @Email String customerEmail) {
    }

    record OrderResponse(Long id, String sku, int quantity, String customerEmail, BigDecimal totalNaira,
                         String status, String reason, Instant createdAt) {

        static OrderResponse from(Order order) {
            return new OrderResponse(order.getId(), order.getSku(), order.getQuantity(), order.getCustomerEmail(),
                    order.getTotalNaira(), order.getStatus().name(), order.getReason(), order.getCreatedAt());
        }
    }
}
