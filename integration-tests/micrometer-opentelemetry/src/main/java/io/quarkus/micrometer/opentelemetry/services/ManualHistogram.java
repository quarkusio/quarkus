package io.quarkus.micrometer.opentelemetry.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

@ApplicationScoped
public class ManualHistogram {
    @Inject
    MeterRegistry registry;

    public void recordHistogram() {
        DistributionSummary summary = DistributionSummary.builder("testSummary")
                .description("This is a test distribution summary")
                .baseUnit("things")
                .tags("tag", "value")
                .serviceLevelObjectives(1, 10, 100, 1000)
                .distributionStatisticBufferLength(10)
                .register(registry);

        summary.record(0.5);
        summary.record(5);
        summary.record(50);
        summary.record(500);
    }
}
