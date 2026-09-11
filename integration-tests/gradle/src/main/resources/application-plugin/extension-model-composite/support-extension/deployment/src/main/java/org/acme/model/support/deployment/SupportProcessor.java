package org.acme.model.support.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SupportProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("support-model-extension");
    }
}
