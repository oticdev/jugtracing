package com.naijajug.tracing.order.inventory;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Registers {@link InventoryClient} as an HTTP service in the "inventory-service" group.
 * <p>
 * Spring Boot builds the group's RestClient from its auto-configured builder, which carries the
 * ObservationRegistry, so trace context is propagated on outgoing calls. Spring Cloud sees the
 * {@code lb://} base URL ({@code spring.http.serviceclient.inventory-service.base-url}) and adds
 * load balancing through Eureka.
 */
@Configuration
@ImportHttpServices(group = "inventory-service", types = InventoryClient.class)
class InventoryClientConfig {
}
