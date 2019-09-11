package io.quarkus.yaml.configuration.deployment;

import static io.quarkus.yaml.configuration.runtime.YamlConfigConstants.APPLICATION_YML_FILE;
import static io.quarkus.yaml.configuration.runtime.YamlConfigConstants.MICROPROFILE_CONFIG_YML_FILE;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;

public class YamlConfigurationProcessor {

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFile,
            ApplicationArchivesBuildItem applicationArchives) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.CONFIG_YAML));

        hotDeploymentWatchedFile.produce(new HotDeploymentWatchedFileBuildItem(APPLICATION_YML_FILE));
        hotDeploymentWatchedFile.produce(new HotDeploymentWatchedFileBuildItem(MICROPROFILE_CONFIG_YML_FILE));

        if (applicationArchives.getRootArchive().getChildPath(APPLICATION_YML_FILE) == null
                && applicationArchives.getRootArchive().getChildPath(MICROPROFILE_CONFIG_YML_FILE) == null) {
            throw new ConfigurationError("Unable to find a YAML configuration file. Please add either " + APPLICATION_YML_FILE
                    + " or " + MICROPROFILE_CONFIG_YML_FILE);
        }
    }
}
