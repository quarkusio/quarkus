package io.quarkus.micrometer.runtime.binder.mpmetrics;

import jakarta.inject.Inject;

import org.eclipse.microprofile.metrics.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.test.QuarkusUnitTest;

public class MpMetricRegistrationTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true)
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder.mp-metrics.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false");

    @Inject
    MetricRegistryAdapter mpRegistry;

    @Inject
    MeterRegistry registry;

    @Test
    public void metricsWithSameMetadata() {
        Metadata metadata1 = Metadata.builder().withName("meter").withDescription("description1").build();
        Metadata metadata2 = Metadata.builder().withName("meter").withDescription("description1").build();

        MeterAdapter meter1 = (MeterAdapter) mpRegistry.meter(metadata1);
        MeterAdapter meter2 = (MeterAdapter) mpRegistry.meter(metadata2);

        Assertions.assertSame(meter1, meter2);
    }

    @Test
    public void metricsWithDifferentType() {
        Metadata metadata1 = Metadata.builder().withName("metric1")
                .withDescription("description1").build();
        Metadata metadata2 = Metadata.builder().withName("metric1")
                .withDescription("description2").build();

        mpRegistry.histogram(metadata1);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            mpRegistry.meter(metadata2);
        });
    }

    @Test
    public void wrongTypeInMetadata() {
        Metadata metadata1 = Metadata.builder().withName("metric1")
                .withDescription("description1").build();

        Metadata metadata2 = Metadata.builder()
                .withName("metric1")
                .withType(MetricType.COUNTER)
                .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            mpRegistry.histogram(metadata2);
        });
    }

    @Test
    public void descriptionChanged() {
        Metadata metadata1 = Metadata.builder().withName("metric1")
                .withDescription("description1").build();
        Metadata metadata2 = Metadata.builder().withName("metric1")
                .withDescription("description2").build();

        // harmless re-registration
        mpRegistry.histogram(metadata1);
        HistogramAdapter histogram = (HistogramAdapter) mpRegistry.histogram(metadata1);

        Assertions.assertEquals("description1", histogram.getMeter().getId().getDescription(),
                "Description should match first set value");
    }

    @Test
    public void metricsWithSameName() {
        int cmSize = mpRegistry.constructedMeters.size();
        int mdSize = mpRegistry.metadataMap.size();
        Metadata metadata1 = Metadata.builder().withName("mycounter").withDescription("description1").build();

        CounterAdapter counter1 = (CounterAdapter) mpRegistry.counter(metadata1);
        CounterAdapter counter2 = (CounterAdapter) mpRegistry.counter("mycounter", new Tag("color", "blue"));

        Assertions.assertNotEquals(counter1, counter2);
        Assertions.assertEquals("description1", counter1.getMeter().getId().getDescription(),
                "Description should match shared value");
        Assertions.assertEquals("description1", counter2.getMeter().getId().getDescription(),
                "Description should match shared value");

        mpRegistry.remove("mycounter");

        Assertions.assertEquals(cmSize, mpRegistry.constructedMeters.size(),
                "Both counters should have been removed");
        Assertions.assertEquals(mdSize, mpRegistry.metadataMap.size(),
                "mycounter metadata should have been removed");
    }
}
