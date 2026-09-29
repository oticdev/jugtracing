package com.naijajug.tracing.order.events;

import com.naijajug.tracing.order.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, OrderPlacedEvent> kafka;
    private final String topic;

    OrderEventPublisher(KafkaTemplate<String, OrderPlacedEvent> kafka,
                        @Value("${app.kafka.topics.order-events}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    public void orderPlaced(Order order) {
        OrderPlacedEvent event = OrderPlacedEvent.from(order);
        kafka.send(topic, String.valueOf(order.getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event for order {}", order.getId(), ex);
                    } else {
                        log.info("Published {} event for order {} to {}", event.status(), order.getId(), topic);
                    }
                });
    }
}
