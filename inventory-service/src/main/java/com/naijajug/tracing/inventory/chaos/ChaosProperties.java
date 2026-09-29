package com.naijajug.tracing.inventory.chaos;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Knobs for making inventory slow or flaky on stage. Mutable so they can be
 * changed at runtime through {@code POST /api/inventory/chaos}.
 */
@ConfigurationProperties("inventory.chaos")
public class ChaosProperties {

    /** Extra delay added to every inventory request. */
    private volatile long latencyMs = 0;

    /** Probability (0.0 - 1.0) that a request fails with 503. */
    private volatile double failureRate = 0.0;

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = Math.max(0, latencyMs);
    }

    public double getFailureRate() {
        return failureRate;
    }

    public void setFailureRate(double failureRate) {
        this.failureRate = Math.clamp(failureRate, 0.0, 1.0);
    }
}
