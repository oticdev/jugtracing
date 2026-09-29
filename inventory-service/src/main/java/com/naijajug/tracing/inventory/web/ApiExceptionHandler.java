package com.naijajug.tracing.inventory.web;

import com.naijajug.tracing.inventory.chaos.ChaosException;
import com.naijajug.tracing.inventory.product.InsufficientStockException;
import com.naijajug.tracing.inventory.product.ProductNotFoundException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    private final ObservationRegistry observationRegistry;

    ApiExceptionHandler(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @ExceptionHandler
    ProblemDetail notFound(ProductNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler
    ProblemDetail insufficientStock(InsufficientStockException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler
    ProblemDetail chaos(ChaosException e) {
        // Handled exceptions never reach the HTTP server span by themselves.
        // The server observation is still current here: record the error so the span is marked as an error.
        Observation current = observationRegistry.getCurrentObservation();
        if (current != null) {
            current.error(e);
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
}
