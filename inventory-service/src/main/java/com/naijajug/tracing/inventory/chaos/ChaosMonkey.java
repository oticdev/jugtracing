package com.naijajug.tracing.inventory.chaos;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class ChaosMonkey {

    private static final Logger log = LoggerFactory.getLogger(ChaosMonkey.class);

    private final ChaosProperties properties;
    private final Tracer tracer;

    public ChaosMonkey(ChaosProperties properties, Tracer tracer) {
        this.properties = properties;
        this.tracer = tracer;
    }

    public void maybeMisbehave() {
        long latencyMs = properties.getLatencyMs();
        if (latencyMs > 0) {
            log.warn("Chaos: sleeping {} ms", latencyMs);
            tagCurrentSpan("chaos.latency_ms", String.valueOf(latencyMs));
            try {
                Thread.sleep(latencyMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (ThreadLocalRandom.current().nextDouble() < properties.getFailureRate()) {
            log.error("Chaos: failing request (failure-rate={})", properties.getFailureRate());
            tagCurrentSpan("chaos.failure", "true");
            throw new ChaosException();
        }
    }

    /** Makes injected chaos visible (and searchable with TraceQL) on the current HTTP span. */
    private void tagCurrentSpan(String key, String value) {
        Span span = tracer.currentSpan();
        if (span != null) {
            span.tag("chaos.injected", "true");
            span.tag(key, value);
        }
    }
}
