package io.quarkus.micrometer.runtime.binder.stork;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.smallrye.stork.api.observability.StorkObservation;

class StorkObservationCollectorBeanTest {

    @Test
    void createsOneObservationPerOperation() {
        StorkObservationCollectorBean collector = new StorkObservationCollectorBean(new SimpleMeterRegistry());

        assertNotSame(collector.create("svc", "static", "round-robin"),
                collector.create("svc", "static", "round-robin"));
    }

    @Test
    void recordsFullDurations() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        StorkObservationCollectorBean collector = new StorkObservationCollectorBean(registry);
        StorkObservation observation = Mockito.mock(StorkObservation.class);
        Mockito.when(observation.getServiceName()).thenReturn("svc");
        Mockito.when(observation.getDiscoveredInstancesCount()).thenReturn(2);
        Mockito.when(observation.getServiceDiscoveryDuration()).thenReturn(Duration.ofMillis(1500));
        Mockito.when(observation.getServiceSelectionDuration()).thenReturn(Duration.ofMillis(2250));

        collector.complete(observation);

        assertEquals(2, registry.get("stork.service-discovery.instances.count").tag("service-name", "svc").counter().count());
        assertEquals(1500, registry.get("stork.service-discovery.duration").tag("service-name", "svc").timer()
                .totalTime(TimeUnit.MILLISECONDS));
        assertEquals(2250, registry.get("stork.service-selection.duration").tag("service-name", "svc").timer()
                .totalTime(TimeUnit.MILLISECONDS));
    }
}
