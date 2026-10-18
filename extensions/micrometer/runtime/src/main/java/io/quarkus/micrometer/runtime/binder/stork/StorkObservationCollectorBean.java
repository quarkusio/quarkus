package io.quarkus.micrometer.runtime.binder.stork;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.smallrye.stork.api.observability.ObservationCollector;
import io.smallrye.stork.api.observability.StorkEventHandler;
import io.smallrye.stork.api.observability.StorkObservation;

@ApplicationScoped
@Typed(ObservationCollector.class)
public class StorkObservationCollectorBean implements ObservationCollector, StorkEventHandler {

    public static final String METRICS_SUFFIX = "-metrics";
    public final static Map<String, StorkObservation> STORK_METRICS = new ConcurrentHashMap<>();
    private final Meter.MeterProvider<Counter> instanceCounter;
    private final Meter.MeterProvider<Timer> serviceDiscoveryTimer;
    private final Meter.MeterProvider<Timer> serviceSelectionTimer;
    private final Meter.MeterProvider<Counter> serviceDiscoveryFailures;
    private final Meter.MeterProvider<Counter> serviceSelectionFailures;

    public StorkObservationCollectorBean(final MeterRegistry registry) {

        this.instanceCounter = Counter
                .builder("stork.service-discovery.instances.count")
                .description("The number of service instances discovered")
                .withRegistry(registry);

        this.serviceDiscoveryTimer = Timer
                .builder("stork.service-discovery.duration")
                .description("The duration of the discovery operation")
                .withRegistry(registry);

        this.serviceSelectionTimer = Timer
                .builder("stork.service-selection.duration")
                .description("The duration of the selection operation")
                .withRegistry(registry);

        this.serviceDiscoveryFailures = Counter
                .builder("stork.service-discovery.failures")
                .description("The number of failures during service discovery")
                .withRegistry(registry);

        this.serviceSelectionFailures = Counter
                .builder("stork.service-selection.failures")
                .description("The number of failures during service selection")
                .withRegistry(registry);
    }

    @Override
    public StorkObservation create(String serviceName, String serviceDiscoveryType,
            String serviceSelectionType) {
        return STORK_METRICS.computeIfAbsent(serviceName + METRICS_SUFFIX,
                key -> new StorkObservation(serviceName, serviceDiscoveryType, serviceSelectionType,
                        this));
    }

    @Override
    public void complete(StorkObservation observation) {
        Tags tags = Tags.of(Tag.of("service-name", observation.getServiceName()));

        int count = observation.getDiscoveredInstancesCount();
        this.instanceCounter.withTags(tags).increment(Math.max(count, 0));
        this.serviceDiscoveryTimer.withTags(tags).record(observation.getServiceDiscoveryDuration().getNano(),
                TimeUnit.NANOSECONDS);
        this.serviceSelectionTimer.withTags(tags).record(observation.getServiceSelectionDuration().getNano(),
                TimeUnit.NANOSECONDS);

        Counter ssf = this.serviceSelectionFailures.withTags(tags);
        Counter sdf = this.serviceDiscoveryFailures.withTags(tags);
        if (observation.failure() != null) {
            if (observation.isServiceDiscoverySuccessful()) {
                ssf.increment();
            } else {// SD failure
                sdf.increment();
                // This forces the creation of the counter if it does not exist.
                ssf.increment(0);
            }
        } else {
            // This forces the creation of the counters if they do not exist.
            ssf.increment(0);
            sdf.increment(0);
        }

    }
}
