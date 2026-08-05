package io.quarkus.stork;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import io.smallrye.stork.api.observability.ObservationCollector;
import io.vertx.core.Vertx;

public class QuarkusStorkObservableInfrastructure extends QuarkusStorkInfrastructure {
    private volatile ObservationCollector observationCollector;

    public QuarkusStorkObservableInfrastructure(Vertx vertx) {
        super(vertx);
    }

    @Override
    public ObservationCollector getObservationCollector() {
        ObservationCollector collector = observationCollector;
        if (collector != null) {
            return collector;
        }
        try {
            Instance<ObservationCollector> instance = CDI.current().select(ObservationCollector.class);
            if (instance.isResolvable()) {
                collector = instance.get();
            }
        } catch (IllegalStateException e) {
            // CDI container not ready yet — return no-op without caching so Micrometer
            // can still be picked up once the container is available.
            return super.getObservationCollector();
        }
        if (collector == null) {
            // Micrometer (or another ObservationCollector) is not present.
            collector = super.getObservationCollector();
        }
        observationCollector = collector;
        return collector;
    }
}
