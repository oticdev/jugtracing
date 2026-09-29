package com.naijajug.tracing.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final EmailSender emailSender;

    OrderEventsListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-events}")
    void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received {} event for order {}", event.status(), event.orderId());
        String subject = switch (event.status()) {
            case "CONFIRMED" -> "Your order #%d is confirmed".formatted(event.orderId());
            default -> "Sorry, we couldn't fulfil order #%d".formatted(event.orderId());
        };
        emailSender.send(event.customerEmail(), subject);
    }
}
