package io.quarkus.jaeger.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

// Executed even if the extension is disabled, see https://github.com/quarkusio/quarkus/pull/26966/
public class JaegerAlwaysEnabledProcessor {

    @BuildStep
    public FeatureBuildItem build() {
        return new FeatureBuildItem(Feature.JAEGER);
    }

}
