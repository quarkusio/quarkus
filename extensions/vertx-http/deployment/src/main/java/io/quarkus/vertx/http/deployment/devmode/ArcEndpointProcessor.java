package io.quarkus.vertx.http.deployment.devmode;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.devmode.ArcEndpointRecorder;

public class ArcEndpointProcessor {

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void registerRoutes(ArcEndpointRecorder recorder, BuildProducer<RouteBuildItem> routes) {
        routes.produce(new RouteBuildItem("/arc/beans", recorder.createBeansHandler()));
        routes.produce(new RouteBuildItem("/arc/observers", recorder.createObserversHandler()));
    }

}
