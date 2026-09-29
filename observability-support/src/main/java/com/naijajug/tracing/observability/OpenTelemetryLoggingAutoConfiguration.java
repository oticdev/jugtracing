package com.naijajug.tracing.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * The OTEL Logback appender (see {@code logback-otel.xml}) buffers log events until it is given
 * the OpenTelemetry SDK. Spring Boot creates the SDK, so we hand it over once the context is up.
 */
@AutoConfiguration
@ConditionalOnClass(OpenTelemetryAppender.class)
public class OpenTelemetryLoggingAutoConfiguration {

    @Bean
    InitializingBean openTelemetryAppenderInstaller(ObjectProvider<OpenTelemetry> openTelemetry) {
        return () -> openTelemetry.ifAvailable(OpenTelemetryAppender::install);
    }
}
