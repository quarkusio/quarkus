package io.quarkus.deployment.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.runtime.LiveReloadConfig;

class DevModeBuildStep {
    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> watchChanges(LiveReloadConfig liveReloadConfig) {
        List<String> names = new ArrayList<>();
        liveReloadConfig.watchedResources().ifPresent(names::addAll);
        return names.stream().map(HotDeploymentWatchedFileBuildItem::new).collect(Collectors.toList());
    }
}
