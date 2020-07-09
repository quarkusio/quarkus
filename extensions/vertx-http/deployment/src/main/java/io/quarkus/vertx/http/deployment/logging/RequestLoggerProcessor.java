package io.quarkus.vertx.http.deployment.logging;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.logging.RequestLoggerRecorder;

public class RequestLoggerProcessor {

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupRequestLoggerHandler(
            RequestLoggerRecorder recorder,
            BuildProducer<FilterBuildItem> filterBuildItemBuildProducer,
            HttpBuildTimeConfig buildTimeConfig) {
        if (buildTimeConfig.requestLogger.enabled) {
            filterBuildItemBuildProducer.produce(
                    new FilterBuildItem(recorder.handler(buildTimeConfig.requestLogger.format), FilterBuildItem.LOGGER));
        }
    }
}
