package io.quarkus.resteasy.jsonb.deployment;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class ResteasyJsonbProcessor {

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY_JSONB));
    }

    @BuildStep
    void capabilities(BuildProducer<CapabilityBuildItem> capability) {
        capability.produce(new CapabilityBuildItem(Capabilities.RESTEASY_JSON_EXTENSION));
        capability.produce(new CapabilityBuildItem(Capabilities.REST_JSONB));
    }
}
