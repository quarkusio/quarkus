package io.quarkus.hibernate.spatial;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;

// Executed even if the extension is disabled, see https://github.com/quarkusio/quarkus/pull/26966/
public final class HibernateSpatialAlwaysEnabledProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.HIBERNATE_SPATIAL);
    }

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.spatial.integration.SpatialService",
                "Hibernate Spatial integration enabled"));
    }

}
