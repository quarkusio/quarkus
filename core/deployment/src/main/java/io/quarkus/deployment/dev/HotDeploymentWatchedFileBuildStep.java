package io.quarkus.deployment.dev;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.dev.testing.TestWatchedFiles;

public class HotDeploymentWatchedFileBuildStep {

    @BuildStep
    ServiceStartBuildItem setupWatchedFileHotDeployment(List<HotDeploymentWatchedFileBuildItem> files,
            LaunchModeBuildItem launchModeBuildItem) {
        // TODO: this should really be an output of the RuntimeRunner
        RuntimeUpdatesProcessor processor = RuntimeUpdatesProcessor.INSTANCE;
        if (processor != null || launchModeBuildItem.isAuxiliaryApplication()) {

            Map<String, Boolean> watchedFilePaths = files.stream().filter(HotDeploymentWatchedFileBuildItem::hasLocation)
                    .collect(Collectors.toMap(HotDeploymentWatchedFileBuildItem::getLocation,
                            HotDeploymentWatchedFileBuildItem::isRestartNeeded,
                            (isRestartNeeded1, isRestartNeeded2) -> isRestartNeeded1 || isRestartNeeded2));

            List<Entry<Predicate<String>, Boolean>> watchedFilePredicates = files.stream()
                    .filter(HotDeploymentWatchedFileBuildItem::hasLocationPredicate)
                    .map(f -> Map.entry(f.getLocationPredicate(), f.isRestartNeeded()))
                    .collect(Collectors.toUnmodifiableList());

            if (launchModeBuildItem.isAuxiliaryApplication()) {
                TestWatchedFiles.setWatchedFilePaths(watchedFilePaths, watchedFilePredicates);
            } else {
                processor.setWatchedFilePaths(watchedFilePaths, watchedFilePredicates, false);
            }
        }
        return null;
    }
}
