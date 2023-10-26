package io.quarkus.micrometer.runtime.binder.stork;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.smallrye.stork.api.observability.ObservationCollector;
import io.smallrye.stork.api.observability.StorkEventHandler;
import io.smallrye.stork.api.observability.StorkObservation;

@ApplicationScoped
@Typed(ObservationCollector.class)
public class StorkObservationCollectorBean implements ObservationCollector, StorkEventHandler {

    public static final String METRICS_SUFIX = "-metrics";
    final MeterRegistry registry = Metrics.globalRegistry;

    public final static Map<String, StorkObservation> STORK_METRICS = new ConcurrentHashMap<>();

    @Override
    public StorkObservation create(String serviceName, String serviceDiscoveryType,
            String serviceSelectionType) {
        return STORK_METRICS.computeIfAbsent(serviceName + METRICS_SUFIX,
                key -> new StorkObservation(serviceName, serviceDiscoveryType, serviceSelectionType,
                        this));
    }

    @Override
    public void complete(StorkObservation observation) {
        Tags tags = Tags.of(Tag.of("service-name", observation.getServiceName()));

        Counter instanceCounter = Counter.builder("stork.service-discovery.instances.count")
                .description("The number of service instances discovered")
                .tags(tags)
                .register(registry);

        Timer serviceDiscoveryTimer = Timer
                .builder("stork.service-discovery.duration")
                .description("The duration of the discovery operation")
                .tags(tags)
                .register(registry);

        Timer serviceSelectionTimer = Timer
                .builder("stork.service-selection.duration")
                .description("The duration of the selection operation ")
                .tags(tags)
                .register(registry);

        Counter serviceDiscoveryFailures = Counter
                .builder("stork.service-discovery.failures")
                .description("The number of failures during service discovery").tags(tags)
                .register(registry);

        Counter serviceSelectionFailures = Counter
                .builder("stork.service-selection.failures")
                .description("The number of failures during service selection.").tags(tags)
                .register(registry);

        instanceCounter.increment(observation.getDiscoveredInstancesCount());
        serviceDiscoveryTimer.record(observation.getServiceDiscoveryDuration().getNano(), TimeUnit.NANOSECONDS);
        serviceSelectionTimer.record(observation.getServiceSelectionDuration().getNano(), TimeUnit.NANOSECONDS);

        if (observation.failure() != null) {
            if (observation.isServiceDiscoverySuccessful()) {
                serviceSelectionFailures.increment();
            } else {// SD failure
                serviceDiscoveryFailures.increment();
            }
        }

    }
}
