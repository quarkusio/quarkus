package io.quarkus.stork;

import jakarta.inject.Singleton;

import io.smallrye.stork.api.observability.ObservationCollector;
import io.vertx.core.Vertx;

@Singleton
public class QuarkusStorkObservableInfrastructure extends QuarkusStorkInfrastructure {

    private final ObservationCollector observationCollector;

    public QuarkusStorkObservableInfrastructure(Vertx vertx, ObservationCollector observationCollector) {
        super(vertx);
        this.observationCollector = observationCollector;
    }

    @Override
    public ObservationCollector getObservationCollector() {
        return observationCollector;
    }
}
