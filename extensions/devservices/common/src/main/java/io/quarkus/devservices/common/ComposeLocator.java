package io.quarkus.devservices.common;

import static io.quarkus.devservices.common.ContainerUtil.getShortId;
import static io.quarkus.devservices.common.Labels.DOCKER_COMPOSE_SERVICE;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;

import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.dev.devservices.RunningContainer;
import io.quarkus.runtime.LaunchMode;

/**
 * Util class to Locate a running container in a compose dev services project.
 */
public class ComposeLocator {

    private static final Logger log = Logger.getLogger(ComposeLocator.class);

    public static Optional<ContainerAddress> locateContainer(DevServicesComposeProjectBuildItem composeProject,
            List<String> images, int port, LaunchMode launchMode, boolean useSharedNetwork) {
        if (launchMode != LaunchMode.NORMAL) {
            return composeProject.locate(images, port)
                    .map(runningContainer -> {
                        String serviceName = getServiceName(runningContainer);
                        ContainerAddress containerAddress = new ContainerAddress(runningContainer,
                                useSharedNetwork ? serviceName : DockerClientFactory.instance().dockerHostIpAddress(),
                                useSharedNetwork ? port
                                        : runningContainer.getPortMapping(port).orElseThrow(
                                                () -> new IllegalStateException("No public port found for " + port)));
                        log.infof("Compose Dev Service container found: %s (%s). Connecting to: %s.",
                                getShortId(containerAddress.getId()),
                                containerAddress.getRunningContainer().containerInfo().imageName(),
                                containerAddress.getUrl());
                        return containerAddress;
                    });
        }
        return Optional.empty();
    }

    public static String getServiceName(RunningContainer runningContainer) {
        return runningContainer.containerInfo().labels().get(DOCKER_COMPOSE_SERVICE);
    }

    public static List<RunningContainer> locateContainer(DevServicesComposeProjectBuildItem composeProject,
            List<String> images, LaunchMode launchMode) {
        if (launchMode != LaunchMode.NORMAL) {
            return composeProject.locate(images);
        }
        return Collections.emptyList();
    }

}
