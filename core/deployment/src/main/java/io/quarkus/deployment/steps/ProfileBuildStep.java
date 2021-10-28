package io.quarkus.deployment.steps;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.runtime.LaunchMode;

public class ProfileBuildStep {

    @BuildStep
    RunTimeConfigurationDefaultBuildItem defaultProfile(LaunchModeBuildItem launchModeBuildItem) {
        return new RunTimeConfigurationDefaultBuildItem("quarkus.profile",
                getProfileValue(launchModeBuildItem.getLaunchMode()));
    }

    @BuildStep
    void watchProfileSpecificFile(LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<HotDeploymentWatchedFileBuildItem> items) {
        String activeProfile = getProfileValue(launchModeBuildItem.getLaunchMode());
        String suffixedApplicationProperties = String.format("application-%s.properties", activeProfile);
        items.produce(new HotDeploymentWatchedFileBuildItem(suffixedApplicationProperties));
        items.produce(new HotDeploymentWatchedFileBuildItem(
                Paths.get(String.format(".env-%s", activeProfile)).toAbsolutePath().toString()));
        items.produce(new HotDeploymentWatchedFileBuildItem(Paths
                .get(System.getProperty("user.dir"), "config", suffixedApplicationProperties).toAbsolutePath().toString()));
        Optional<List<String>> locations = ConfigProvider.getConfig().getOptionalValues(SMALLRYE_CONFIG_LOCATIONS,
                String.class);
        if (locations.isPresent()) {
            for (String location : locations.get()) {
                items.produce(new HotDeploymentWatchedFileBuildItem(appendProfileToFilename(location, activeProfile)));
            }
        }
    }

    private String appendProfileToFilename(String path, String activeProfile) {
        String pathWithoutExtension = FilenameUtils.removeExtension(path);
        return String.format("%s-%s.%s", pathWithoutExtension, activeProfile, FilenameUtils.getExtension(path));
    }

    private String getProfileValue(LaunchMode launchMode) {
        if (launchMode == LaunchMode.DEVELOPMENT) {
            return "dev";
        } else if (launchMode == LaunchMode.TEST) {
            return "test";
        }
        return "prod";
    }
}
