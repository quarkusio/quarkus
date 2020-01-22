package io.quarkus.dev;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;

public class HotDeploymentConfigFileBuildStep {

    @BuildStep
    ServiceStartBuildItem setupConfigFileHotDeployment(List<HotDeploymentWatchedFileBuildItem> files) {
        // TODO: this should really be an output of the RuntimeRunner
        RuntimeUpdatesProcessor processor = IsolatedDevModeMain.runtimeUpdatesProcessor;
        if (processor != null) {
            Map<String, Boolean> watchedFilePaths = files.stream()
                    .collect(Collectors.toMap(HotDeploymentWatchedFileBuildItem::getLocation,
                            HotDeploymentWatchedFileBuildItem::isRestartNeeded));
            processor.setWatchedFilePaths(watchedFilePaths);
        }
        return null;
    }
}
