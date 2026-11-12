package org.acme.ext.deployment;

import org.acme.ext.AcmeExtConfig;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;

class AcmeExtProcessor {

    private static final String FEATURE = "acme-ext";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    SystemPropertyBuildItem greeting(AcmeExtConfig config) {
        return new SystemPropertyBuildItem("acme.ext.greeting", config.greeting());
    }
}
