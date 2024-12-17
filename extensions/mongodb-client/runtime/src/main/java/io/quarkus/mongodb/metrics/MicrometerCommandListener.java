package io.quarkus.mongodb.metrics;

import jakarta.inject.Inject;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;

public class MicrometerCommandListener extends MongoMetricsCommandListener {
    @Inject
    public MicrometerCommandListener(MeterRegistry registry) {
        super(registry);
    }

}
