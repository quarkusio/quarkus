package io.quarkus.undertow.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;

public class UndertowLogFilterBuildStep {

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem("org.xnio", "XNIO version"));
        filters.produce(new LogCleanupFilterBuildItem("org.xnio.nio", "XNIO NIO Implementation Version"));
    }
}
