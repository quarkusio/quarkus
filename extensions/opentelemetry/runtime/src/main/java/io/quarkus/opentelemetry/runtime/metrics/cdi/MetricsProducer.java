package io.quarkus.opentelemetry.runtime.metrics.cdi;

import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.common.Clock;
import io.quarkus.arc.DefaultBean;

@Singleton
public class MetricsProducer {
    @Produces
    @ApplicationScoped
    @DefaultBean
    public Meter getMeter() {
        return GlobalOpenTelemetry.getMeter(INSTRUMENTATION_NAME);
    }

    @Produces
    @Singleton
    @DefaultBean
    public Clock getClock() {
        return Clock.getDefault();
    }
}
