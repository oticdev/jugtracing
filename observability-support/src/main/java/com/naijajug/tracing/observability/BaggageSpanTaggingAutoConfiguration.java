package com.naijajug.tracing.observability;

import io.micrometer.tracing.otel.bridge.BaggageTaggingSpanProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Copies baggage fields listed in {@code management.tracing.baggage.tag-fields} onto every span
 * as it starts, including baggage that arrived from upstream (HTTP/Kafka headers).
 * <p>
 * Spring Boot only applies tag-fields to baggage created in code, so without this the
 * {@code customer.tier} header would reach the logs but not the spans (TraceQL, tail sampling).
 */
@AutoConfiguration
@ConditionalOnClass(BaggageTaggingSpanProcessor.class)
public class BaggageSpanTaggingAutoConfiguration {

    @Bean
    BaggageTaggingSpanProcessor baggageTaggingSpanProcessor(Environment environment) {
        List<String> tagFields = Binder.get(environment)
                .bind("management.tracing.baggage.tag-fields", Bindable.listOf(String.class))
                .orElse(List.of());
        return new BaggageTaggingSpanProcessor(tagFields);
    }
}
