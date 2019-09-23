package io.quarkus.resteasy.jsonb.deployment;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class ResteasyJsonbProcessor {

    @BuildStep(providesCapabilities = { Capabilities.RESTEASY_JSON_EXTENSION, "io.quarkus.resteasy.jsonb" })
    void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY_JSONB));
    }
}
