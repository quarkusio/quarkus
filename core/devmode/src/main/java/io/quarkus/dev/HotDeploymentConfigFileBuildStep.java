package io.quarkus.dev;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;

public class HotDeploymentConfigFileBuildStep {

    @BuildStep
    ServiceStartBuildItem setupConfigFileHotDeployment(List<HotDeploymentWatchedFileBuildItem> files) {
        //TODO: this should really be an output of the RuntimeRunner
        Set<String> fileSet = files.stream().map(HotDeploymentWatchedFileBuildItem::getLocation).collect(Collectors.toSet());
        RuntimeUpdatesProcessor processor = DevModeMain.runtimeUpdatesProcessor;
        if (processor != null) {
            processor.setWatchedFilePaths(fileSet);
        }
        return null;
    }
}
