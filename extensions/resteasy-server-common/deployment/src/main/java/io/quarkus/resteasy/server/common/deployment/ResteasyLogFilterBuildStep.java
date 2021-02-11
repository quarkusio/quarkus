package io.quarkus.resteasy.server.common.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;

/**
 * Processor that sets up log filters for RESTEasy
 */
public class ResteasyLogFilterBuildStep {

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem("org.jboss.resteasy.resteasy_jaxrs.i18n", "RESTEASY002225"));
    }
}
