package io.quarkus.arc.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentConfigFileBuildItem;

public class HotDeploymentConfigBuildStep {

    @BuildStep
    HotDeploymentConfigFileBuildItem configFile() {
        return new HotDeploymentConfigFileBuildItem("META-INF/beans.xml");
    }

}
