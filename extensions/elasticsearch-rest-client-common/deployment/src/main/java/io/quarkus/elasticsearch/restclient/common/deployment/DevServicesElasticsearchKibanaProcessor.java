package io.quarkus.elasticsearch.restclient.common.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class DevServicesElasticsearchKibanaProcessor {

    static volatile DevservicesElasticsearchDashboardProcessorStrategy strategy = DevservicesElasticsearchDashboardProcessorStrategy.kibana();

    @BuildStep
    public DevServicesResultBuildItem startElasticsearchDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            LaunchModeBuildItem launchMode,
            ElasticsearchCommonBuildTimeConfig configuration,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            List<DevservicesElasticsearchConnectionBuildItem> elasticsearchConnectionBuildItems,
            List<DevservicesElasticsearchBuildItem> devservicesElasticsearchBuildItems,
            DevServicesConfig devServicesConfig) throws BuildException {

        return strategy.startElasticsearchDevService(dockerStatusBuildItem, composeProjectBuildItem, launchMode, configuration,
                devServicesSharedNetworkBuildItem, consoleInstalledBuildItem, closeBuildItem, loggingSetupBuildItem,
                elasticsearchConnectionBuildItems, devservicesElasticsearchBuildItems, devServicesConfig);
    }
}
