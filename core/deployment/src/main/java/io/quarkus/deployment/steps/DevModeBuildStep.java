package io.quarkus.deployment.steps;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.runtime.LiveReloadConfig;

class DevModeBuildStep {

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> watchChanges(LiveReloadConfig config) {
        List<String> names = new ArrayList<>();
        names.add("META-INF/microprofile-config.properties");
        names.add("application.properties");
        names.add(Paths.get(".env").toAbsolutePath().toString());
        config.watchedResources.ifPresent(names::addAll);
        return names.stream().map(HotDeploymentWatchedFileBuildItem::new).collect(Collectors.toList());
    }
}
