package io.quarkus.devservices.common;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiPredicate;

import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;

import io.quarkus.runtime.LaunchMode;

public class ContainerLocator {

    private static final Logger log = Logger.getLogger(ContainerLocator.class);
    private static final BiPredicate<ContainerPort, Integer> hasMatchingPort = (containerPort,
            port) -> containerPort.getPrivatePort() != null &&
                    containerPort.getPublicPort() != null &&
                    containerPort.getPrivatePort().equals(port);

    private final String devServiceLabel;
    private final int port;

    public ContainerLocator(String devServiceLabel, int port) {
        this.devServiceLabel = devServiceLabel;
        this.port = port;
    }

    private Optional<Container> lookup(String expectedLabelValue) {
        return DockerClientFactory.lazyClient().listContainersCmd().exec().stream()
                .filter(container -> expectedLabelValue.equals(container.getLabels().get(devServiceLabel)))
                .findAny();
    }

    private Optional<ContainerPort> getMappedPort(Container container, int port) {
        return Arrays.stream(container.getPorts())
                .filter(containerPort -> hasMatchingPort.test(containerPort, port))
                .findAny();
    }

    public Optional<ContainerAddress> locateContainer(String serviceName, boolean shared, LaunchMode launchMode) {
        if (shared && launchMode == LaunchMode.DEVELOPMENT) {
            return lookup(serviceName)
                    .flatMap(container -> getMappedPort(container, port)
                            .flatMap(containerPort -> Optional.ofNullable(containerPort.getPublicPort())
                                    .map(port -> {
                                        final ContainerAddress containerAddress = new ContainerAddress(
                                                DockerClientFactory.instance().dockerHostIpAddress(),
                                                containerPort.getPublicPort());
                                        log.infof("Dev Services container found: %s (%s). Connecting to: %s.",
                                                container.getId(),
                                                container.getImage(),
                                                containerAddress.getUrl());
                                        return containerAddress;
                                    })));
        } else {
            return Optional.empty();
        }
    }
}
