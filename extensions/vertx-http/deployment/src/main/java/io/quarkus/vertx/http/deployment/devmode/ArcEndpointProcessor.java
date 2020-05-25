package io.quarkus.vertx.http.deployment.devmode;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.devmode.ArcEndpointRecorder;

public class ArcEndpointProcessor {

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void registerRoutes(HttpBuildTimeConfig config, ArcEndpointRecorder recorder, BuildProducer<RouteBuildItem> routes,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints) {
        String basePath = config.consolePath + "/arc";
        String beansPath = basePath + "/beans";
        String observersPath = basePath + "/observers";
        routes.produce(new RouteBuildItem(beansPath, recorder.createBeansHandler()));
        routes.produce(new RouteBuildItem(observersPath, recorder.createObserversHandler()));
        displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(beansPath));
        displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(observersPath));
    }

}
