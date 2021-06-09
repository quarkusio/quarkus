package io.quarkus.deployment.dev;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.dev.testing.TestWatchedFiles;

public class HotDeploymentConfigFileBuildStep {

    @BuildStep
    ServiceStartBuildItem setupConfigFileHotDeployment(List<HotDeploymentWatchedFileBuildItem> files,
            LaunchModeBuildItem launchModeBuildItem) {
        // TODO: this should really be an output of the RuntimeRunner
        RuntimeUpdatesProcessor processor = RuntimeUpdatesProcessor.INSTANCE;
        if (processor != null || launchModeBuildItem.isAuxiliaryApplication()) {
            Map<String, Boolean> watchedFilePaths = files.stream()
                    .collect(Collectors.toMap(HotDeploymentWatchedFileBuildItem::getLocation,
                            HotDeploymentWatchedFileBuildItem::isRestartNeeded,
                            (isRestartNeeded1, isRestartNeeded2) -> isRestartNeeded1 || isRestartNeeded2));
            if (launchModeBuildItem.isAuxiliaryApplication()) {
                TestWatchedFiles.setWatchedFilePaths(watchedFilePaths);
            } else {
                processor.setWatchedFilePaths(watchedFilePaths, false);
            }
        }
        return null;
    }
}
