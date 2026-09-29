package com.naijajug.tracing.inventory.chaos;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/chaos")
class ChaosController {

    private final ChaosProperties properties;

    ChaosController(ChaosProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    ChaosSettings get() {
        return new ChaosSettings(properties.getLatencyMs(), properties.getFailureRate());
    }

    @PostMapping
    ChaosSettings update(@RequestBody ChaosSettings settings) {
        properties.setLatencyMs(settings.latencyMs());
        properties.setFailureRate(settings.failureRate());
        return get();
    }

    record ChaosSettings(long latencyMs, double failureRate) {
    }
}
