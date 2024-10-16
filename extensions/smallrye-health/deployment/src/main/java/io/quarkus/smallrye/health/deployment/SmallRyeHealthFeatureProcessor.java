package io.quarkus.smallrye.health.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SmallRyeHealthFeatureProcessor {

    @BuildStep
    public void defineFeature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_HEALTH));
    }
}
