package io.quarkus.opentelemetry;

import java.util.Map;

import jakarta.enterprise.context.spi.CreationalContext;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.quarkus.arc.BeanDestroyer;

public class OpenTelemetryDestroyer implements BeanDestroyer<OpenTelemetry> {
    @Override
    public void destroy(OpenTelemetry openTelemetry, CreationalContext<OpenTelemetry> creationalContext,
            Map<String, Object> params) {
        if (openTelemetry instanceof OpenTelemetrySdk) {
            var openTelemetrySdk = ((OpenTelemetrySdk) openTelemetry);
            openTelemetrySdk.getSdkTracerProvider().forceFlush();
            openTelemetrySdk.getSdkTracerProvider().shutdown();
        }
    }
}
