package io.quarkus.devservices.common;

import static io.quarkus.devservices.common.ContainerUtil.getShortId;
import static io.quarkus.devservices.common.Labels.DOCKER_COMPOSE_SERVICE;
import static java.util.Arrays.stream;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

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
        return locateContainer(composeProject, images, port, launchMode, useSharedNetwork, OptionalInt.empty());
    }

    public static Optional<ContainerAddress> locateContainer(DevServicesComposeProjectBuildItem composeProject,
            List<String> images, int port, LaunchMode launchMode, boolean useSharedNetwork,
            OptionalInt fixedExposedPort) {
        if (launchMode.isDevServicesSupported()) {
            return composeProject.locate(images, port)
                    .filter(rc -> fixedExposedPort.isEmpty()
                            || rc.getPortMapping(port)
                                    .map(publicPort -> publicPort == fixedExposedPort.getAsInt())
                                    .orElse(false))
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
        return locateContainer(composeProject, images, launchMode, OptionalInt.empty());
    }

    public static List<RunningContainer> locateContainer(DevServicesComposeProjectBuildItem composeProject,
            List<String> images, LaunchMode launchMode, OptionalInt fixedExposedPort) {
        if (launchMode.isDevServicesSupported()) {
            List<RunningContainer> containers = composeProject.locate(images);
            if (fixedExposedPort.isPresent()) {
                return containers.stream()
                        .filter(rc -> stream(rc.containerInfo().exposedPorts())
                                .anyMatch(p -> p.publicPort() == fixedExposedPort.getAsInt()))
                        .toList();
            }
            return containers;
        }
        return Collections.emptyList();
    }

}
