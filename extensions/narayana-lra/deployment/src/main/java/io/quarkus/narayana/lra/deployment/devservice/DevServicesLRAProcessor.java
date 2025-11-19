package io.quarkus.narayana.lra.deployment.devservice;

import static io.quarkus.devservices.common.ConfigureUtil.getDefaultImageNameFor;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.narayana.lra.deployment.LRABuildTimeConfiguration;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Start Narayana LRA coordinator as a dev service.
 */
@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class DevServicesLRAProcessor {

    private static final Logger log = Logger.getLogger(DevServicesLRAProcessor.class);
    private static final String LRA_COORDINATOR_URL_PROPERTY = "quarkus.lra.coordinator-url";

    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-lra-coordinator";
    static final int LRA_COORDINATOR_CONTAINER_PORT = 8080;

    private static final ContainerLocator lraCoordinatorContainerLocator = locateContainerWithLabels(
            LRA_COORDINATOR_CONTAINER_PORT, DEV_SERVICE_LABEL);

    @BuildStep
    public DevServicesResultBuildItem lraCoordinatorDevService(
            LRABuildTimeConfiguration lraBuildTimeConfiguration,
            DevServicesComposeProjectBuildItem compose,
            DockerStatusBuildItem dockerStatusBuildItem,
            LaunchModeBuildItem launchMode,
            List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            DevServicesConfig devServicesConfig) {
        LRACoordinatorDevServicesBuildTimeConfig config = lraBuildTimeConfiguration.devservices();
        if (isDevServiceDisabled(dockerStatusBuildItem, config)) {
            return null;
        }

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig, sharedNetwork);

        if (config.logWarning()) {
            log.warn("Dev Services for LRA requires exposing your application on the 0.0.0.0 host address. " +
                    "Your application will be accessible from your network. You can disable this warning by setting " +
                    "quarkus.lra.devservices.log-warning=false.");
        }

        return lraCoordinatorContainerLocator
                .locateContainer(config.serviceName(), config.shared(), launchMode.getLaunchMode())
                .or(() -> ComposeLocator.locateContainer(compose,
                        List.of(config.imageName().orElseGet(() -> getDefaultImageNameFor("narayana-lra")), "lra-coordinator"),
                        LRA_COORDINATOR_CONTAINER_PORT, launchMode.getLaunchMode(), useSharedNetwork))
                .map(containerAddress -> DevServicesResultBuildItem.discovered()
                        .feature(Feature.NARAYANA_LRA)
                        .containerId(containerAddress.getId())
                        .config(Map.of(LRA_COORDINATOR_URL_PROPERTY, "http://" + containerAddress.getUrl() + "/lra-coordinator",
                                "quarkus.http.host", "0.0.0.0", // Required since the container needs to call the host application
                                "quarkus.lra.base-uri",
                                "http://host.containers.internal:"
                                        + (launchMode.isTest() ? "${quarkus.http.test-port}" : "${quarkus.http.port}")))
                        .build())
                .orElseGet(() -> DevServicesResultBuildItem.owned()
                        .feature(Feature.NARAYANA_LRA)
                        .serviceName(config.serviceName())
                        .serviceConfig(config)
                        .startable(() -> createContainer(compose, config, useSharedNetwork, launchMode))
                        .postStartHook(s -> logDevServiceStarted(s.getConnectionInfo()))
                        .configProvider(Map.of(LRA_COORDINATOR_URL_PROPERTY, Startable::getConnectionInfo,
                                "quarkus.http.host", s -> "0.0.0.0", // Required since the container needs to call the host application
                                "quarkus.lra.base-uri",
                                s -> "http://host.containers.internal:"
                                        + (launchMode.isTest() ? "${quarkus.http.test-port}" : "${quarkus.http.port}")))
                        .build());
    }

    private void logDevServiceStarted(String connectionInfo) {
        log.infof("Dev Services for the LRA coordinator started. Other applications in dev mode will find the " +
                "LRA coordinator automatically. For Quarkus application in production mode, you can connect to " +
                "this coordinator by starting you application with -D%s=%s\n", LRA_COORDINATOR_URL_PROPERTY, connectionInfo);

    }

    private Startable createContainer(DevServicesComposeProjectBuildItem compose,
            LRACoordinatorDevServicesBuildTimeConfig config, boolean useSharedNetwork, LaunchModeBuildItem launchMode) {
        return new LRACoordinatorContainer(
                DockerImageName.parse(config.imageName().orElseGet(() -> getDefaultImageNameFor("narayana-lra"))),
                config.port().orElse(0),
                compose.getDefaultNetworkId(),
                useSharedNetwork)
                .withEnv(config.containerEnv())
                .withSharedServiceLabel(launchMode.getLaunchMode(), config.serviceName());
    }

    private boolean isDevServiceDisabled(DockerStatusBuildItem dockerStatusBuildItem,
            LRACoordinatorDevServicesBuildTimeConfig config) {
        // explicitly disable
        if (!config.enabled()) {
            log.debug("Not starting dev services for the LRA coordinator, as it has been disabled in the config.");
            return true;
        }

        // defined LRA coordinator URL
        if (ConfigUtils.isPropertyNonEmpty(LRA_COORDINATOR_URL_PROPERTY)) {
            log.debugf("Not starting dev services for the LRA coordinator, the \"%s\" is configured",
                    LRA_COORDINATOR_URL_PROPERTY);
            return true;
        }

        // missing docker environment
        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warnf("Couldn't find valid Docker environment, please configure the \"%s\" configuration property.",
                    LRA_COORDINATOR_URL_PROPERTY);
            return true;
        }

        return false;
    }
}
