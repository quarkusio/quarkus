package io.quarkus.deployment.steps;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.runtime.LiveReloadConfig;

class DevModeBuildStep {

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> watchChanges(LiveReloadConfig liveReloadConfig) {
        List<String> names = new ArrayList<>();
        names.add("META-INF/microprofile-config.properties");
        names.add("application.properties");
        names.add(Paths.get(".env").toAbsolutePath().toString());
        Optional<List<String>> locations = ConfigProvider.getConfig().getOptionalValues(SMALLRYE_CONFIG_LOCATIONS,
                String.class);
        locations.ifPresent(names::addAll);
        liveReloadConfig.watchedResources.ifPresent(names::addAll);
        return names.stream().map(HotDeploymentWatchedFileBuildItem::new).collect(Collectors.toList());
    }
}
