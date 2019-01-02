package org.jboss.shamrock.arc.deployment;

import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.HotDeploymentConfigFileBuildItem;

public class HotDeploymentConfigBuildStep {

    @BuildStep
    HotDeploymentConfigFileBuildItem configFile() {
        return new HotDeploymentConfigFileBuildItem("META-INF/beans.xml");
    }

}
