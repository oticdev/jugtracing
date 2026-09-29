package com.naijajug.tracing.notification;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Pretends to call an email provider: logs and sleeps, inside its own "send-email" span.
 */
@Component
class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final long simulatedLatencyMs;
    private final ObservationRegistry observationRegistry;

    EmailSender(@Value("${app.email.simulated-latency-ms}") long simulatedLatencyMs,
                ObservationRegistry observationRegistry) {
        this.simulatedLatencyMs = simulatedLatencyMs;
        this.observationRegistry = observationRegistry;
    }

    void send(String to, String subject) {
        Observation.createNotStarted("email.send", observationRegistry)
                .contextualName("send-email")
                .lowCardinalityKeyValue("email.provider", "fake-postmark")
                .highCardinalityKeyValue("email.subject", subject)
                .observe(() -> {
                    try {
                        Thread.sleep(simulatedLatencyMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    log.info("Email sent to {}: \"{}\"", to, subject);
                });
    }
}
