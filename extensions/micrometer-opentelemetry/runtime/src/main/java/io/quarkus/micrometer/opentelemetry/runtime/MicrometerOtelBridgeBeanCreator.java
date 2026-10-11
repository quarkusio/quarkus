package io.quarkus.micrometer.opentelemetry.runtime;

import java.util.concurrent.TimeUnit;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

public class MicrometerOtelBridgeBeanCreator implements BeanCreator<MeterRegistry> {

    @Override
    public MeterRegistry create(SyntheticCreationalContext<MeterRegistry> context) {
        Instance<OpenTelemetry> openTelemetry = context.getInjectedReference(new TypeLiteral<>() {
        });

        if (openTelemetry.isUnsatisfied()) {
            throw new IllegalStateException("OpenTelemetry instance not found");
        }

        MeterRegistry meterRegistry = OpenTelemetryMeterRegistry.builder(openTelemetry.get())
                .setPrometheusMode(false)
                .setMicrometerHistogramGaugesEnabled(true)
                // The time unit should be seconds according to the OpenTelemetry documentation
                .setBaseTimeUnit(TimeUnit.SECONDS)
                .setClock(Clock.SYSTEM)
                .build();
        Metrics.addRegistry(meterRegistry);
        return meterRegistry;
    }
}
