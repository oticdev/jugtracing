package com.naijajug.tracing.observability;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.http.client.observation.ClientRequestObservationContext;

/**
 * Keeps Tempo focused on business traffic: no traces for Eureka heartbeats/registry fetches
 * (every 30s from every service) or for actuator calls.
 */
@AutoConfiguration
@ConditionalOnClass(ServerRequestObservationContext.class)
public class ObservationNoiseFilterAutoConfiguration {

    @Bean
    ObservationPredicate ignoreInfrastructureRequests() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext server) {
                return !server.getCarrier().getRequestURI().startsWith("/actuator");
            }
            if (context instanceof ClientRequestObservationContext client) {
                ClientHttpRequest request = client.getCarrier();
                return request == null || !request.getURI().getPath().startsWith("/eureka");
            }
            return true;
        };
    }
}
