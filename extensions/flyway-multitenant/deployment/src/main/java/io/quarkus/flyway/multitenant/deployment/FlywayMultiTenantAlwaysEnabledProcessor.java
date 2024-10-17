package io.quarkus.flyway.multitenant.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class FlywayMultiTenantAlwaysEnabledProcessor {

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> featureProducer) {
        featureProducer.produce(new FeatureBuildItem(Feature.FLYWAY_MULTITENANT));
    }
}
