package io.quarkus.stork;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import io.smallrye.stork.api.observability.ObservationCollector;
import io.vertx.core.Vertx;

public class QuarkusStorkObservableInfrastructure extends QuarkusStorkInfrastructure {

    public QuarkusStorkObservableInfrastructure(Vertx vertx) {
        super(vertx);
    }

    @Override
    public ObservationCollector getObservationCollector() {
        Instance<ObservationCollector> instance = CDI.current().select(ObservationCollector.class);
        if (instance.isResolvable()) {
            return instance.get();
        }
        return super.getObservationCollector();
    }
}
