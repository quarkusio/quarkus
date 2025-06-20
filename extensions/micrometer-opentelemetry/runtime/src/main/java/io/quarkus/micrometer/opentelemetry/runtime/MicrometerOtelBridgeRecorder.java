package io.quarkus.micrometer.opentelemetry.runtime;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MicrometerOtelBridgeRecorder {

    public Function<SyntheticCreationalContext<MeterRegistry>, MeterRegistry> createBridge() {
        return new Function<>() {
            @Override
            public MeterRegistry apply(SyntheticCreationalContext<MeterRegistry> context) {
                Instance<OpenTelemetry> openTelemetry = context.getInjectedReference(new TypeLiteral<>() {
                });

                if (openTelemetry.isUnsatisfied()) {
                    throw new IllegalStateException("OpenTelemetry instance not found");
                }

                MeterRegistry meterRegistry = OpenTelemetryMeterRegistry.builder(openTelemetry.get())
                        .setPrometheusMode(false)
                        .setMicrometerHistogramGaugesEnabled(true)
                        .setBaseTimeUnit(TimeUnit.MILLISECONDS)
                        .setClock(Clock.SYSTEM)
                        .build();
                Metrics.addRegistry(meterRegistry);
                return meterRegistry;
            }
        };
    }
}
