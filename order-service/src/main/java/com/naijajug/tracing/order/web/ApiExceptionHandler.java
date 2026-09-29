package com.naijajug.tracing.order.web;

import com.naijajug.tracing.order.order.OrderNotFoundException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ObservationRegistry observationRegistry;

    ApiExceptionHandler(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @ExceptionHandler
    ProblemDetail orderNotFound(OrderNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler
    ProblemDetail unknownSku(HttpClientErrorException.NotFound e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Unknown SKU (inventory-service returned 404)");
    }

    /** Inventory is down, erroring or unreachable: that's a bad gateway from our caller's point of view. */
    @ExceptionHandler({HttpServerErrorException.class, ResourceAccessException.class})
    ProblemDetail inventoryUnavailable(Exception e) {
        log.error("Inventory call failed: {}", e.getMessage());
        // We handled the exception, so the HTTP server span would never hear about it.
        // The server observation is still current here: record the error on it, so the span
        // is marked as an error (red in Tempo, counted by TraceQL, kept by tail sampling).
        Observation current = observationRegistry.getCurrentObservation();
        if (current != null) {
            current.error(e);
        }
        String detail = e instanceof HttpServerErrorException serverError
                ? "inventory-service returned " + serverError.getStatusCode().value()
                : "inventory-service is unreachable";
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, detail);
    }
}
