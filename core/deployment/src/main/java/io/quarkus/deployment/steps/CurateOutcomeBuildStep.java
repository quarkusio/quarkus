package io.quarkus.deployment.steps;

import io.quarkus.deployment.BootstrapConfig;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AppModelProviderBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;

public class CurateOutcomeBuildStep {

    BootstrapConfig config;

    @BuildStep
    CurateOutcomeBuildItem curateOutcome(AppModelProviderBuildItem appModelProvider) {
        return new CurateOutcomeBuildItem(appModelProvider.validateAndGet(config));
    }
}
