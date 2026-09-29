package com.naijajug.tracing.order.order;

import com.naijajug.tracing.order.events.OrderEventPublisher;
import com.naijajug.tracing.order.inventory.InventoryClient;
import com.naijajug.tracing.order.inventory.ReserveRequest;
import com.naijajug.tracing.order.inventory.StockResponse;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final InventoryClient inventory;
    private final OrderRepository orders;
    private final OrderEventPublisher events;
    private final ObservationRegistry observationRegistry;

    public OrderService(InventoryClient inventory, OrderRepository orders, OrderEventPublisher events,
                        ObservationRegistry observationRegistry) {
        this.inventory = inventory;
        this.orders = orders;
        this.events = events;
        this.observationRegistry = observationRegistry;
    }

    /**
     * Wrapped in a custom "place-order" span, so the business step shows up in the trace between
     * the incoming HTTP span and the outgoing inventory/Kafka spans.
     * <p>
     * Not @Transactional on purpose: we don't hold a DB transaction open across HTTP calls.
     */
    public Order placeOrder(String sku, int quantity, String customerEmail) {
        Observation observation = Observation.createNotStarted("order.place", observationRegistry)
                .contextualName("place-order")
                .lowCardinalityKeyValue("order.sku", sku)
                .highCardinalityKeyValue("order.quantity", String.valueOf(quantity));

        return observation.observe(() -> {
            log.info("Placing order for {} x {}", quantity, sku);

            StockResponse stock = inventory.getStock(sku);
            observation.event(Observation.Event.of("stock.checked"));

            Order order;
            if (stock.quantityAvailable() < quantity) {
                order = Order.rejected(sku, quantity, customerEmail,
                        "Only %d left in stock".formatted(stock.quantityAvailable()));
            } else {
                order = reserve(stock, quantity, customerEmail);
                if (order.getStatus() == OrderStatus.CONFIRMED) {
                    observation.event(Observation.Event.of("stock.reserved"));
                }
            }

            order = orders.save(order);
            observation.highCardinalityKeyValue("order.id", String.valueOf(order.getId()));
            observation.highCardinalityKeyValue("order.status", order.getStatus().name());
            if (order.getReason() != null) {
                observation.highCardinalityKeyValue("order.reason", order.getReason());
            }
            log.info("Order {} is {}", order.getId(), order.getStatus());

            events.orderPlaced(order);
            return order;
        });
    }

    private Order reserve(StockResponse stock, int quantity, String customerEmail) {
        try {
            inventory.reserve(new ReserveRequest(stock.sku(), quantity));
            BigDecimal total = stock.priceNaira().multiply(BigDecimal.valueOf(quantity));
            return Order.confirmed(stock.sku(), quantity, customerEmail, total);
        } catch (HttpClientErrorException.Conflict e) {
            // Someone else took the stock between our check and the reservation.
            return Order.rejected(stock.sku(), quantity, customerEmail, "Stock ran out during reservation");
        }
    }

    public Order findById(Long id) {
        return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> findAll() {
        return orders.findAll();
    }
}
