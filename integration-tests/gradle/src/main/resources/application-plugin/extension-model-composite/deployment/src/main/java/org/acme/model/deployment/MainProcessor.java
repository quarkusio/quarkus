package org.acme.model.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class MainProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("main-model-extension");
    }
}
