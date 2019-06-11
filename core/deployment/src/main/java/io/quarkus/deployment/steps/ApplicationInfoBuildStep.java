package io.quarkus.deployment.steps;

import io.quarkus.deployment.ApplicationInfoUtil;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

public class ApplicationInfoBuildStep {

    @BuildStep
    public ApplicationInfoBuildItem create() {
        return new ApplicationInfoBuildItem(
                ApplicationInfoUtil.getArtifactId(),
                ApplicationInfoUtil.getVersion());
    }
}
