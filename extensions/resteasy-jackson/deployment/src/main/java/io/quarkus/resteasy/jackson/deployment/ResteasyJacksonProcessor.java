package io.quarkus.resteasy.jackson.deployment;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class ResteasyJacksonProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY_JACKSON));
    }

    @BuildStep
    void capabilities(BuildProducer<CapabilityBuildItem> capability) {
        capability.produce(new CapabilityBuildItem(Capabilities.RESTEASY_JSON_EXTENSION));
        capability.produce(new CapabilityBuildItem(Capabilities.REST_JACKSON));
    }
}
