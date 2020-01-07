package io.quarkus.mutiny.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class MutinyProcessor {

    @BuildStep
    public FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(FeatureBuildItem.MUTINY);
    }
}
