package io.quarkus.deployment.steps;

import io.quarkus.deployment.ApplicationConfig;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

public class ApplicationInfoBuildStep {

    @BuildStep
    public ApplicationInfoBuildItem create(ApplicationConfig applicationConfig) {
        return new ApplicationInfoBuildItem(applicationConfig.name, applicationConfig.version);
    }
}
