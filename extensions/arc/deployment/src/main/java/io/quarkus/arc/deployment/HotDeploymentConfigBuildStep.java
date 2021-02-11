package io.quarkus.arc.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;

public class HotDeploymentConfigBuildStep {

    @BuildStep
    HotDeploymentWatchedFileBuildItem configFile() {
        return new HotDeploymentWatchedFileBuildItem("META-INF/beans.xml");
    }

}
