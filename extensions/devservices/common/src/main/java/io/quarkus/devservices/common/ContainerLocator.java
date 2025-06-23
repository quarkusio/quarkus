package io.quarkus.devservices.common;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;

import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesRegistry;
import io.quarkus.runtime.LaunchMode;

public class ContainerLocator {

    // Read the UUID off the RunningDevServicesRegistry, because it's guaranteed to be parent-first, unlike this class
    public static final BiPredicate<Container, String> IS_NOT_CREATED_BY_US_SELECTOR = (container,
            expectedLabel) -> !RunningDevServicesRegistry.APPLICATION_UUID
                    .equals(container.getLabels().get(Labels.QUARKUS_PROCESS_UUID));

    private static final Logger log = Logger.getLogger(ContainerLocator.class);

    private static boolean hasDevServiceLabels(Container container, Predicate<String> labelPredicate,
            String... devServiceLabels) {
        return Stream.concat(Stream.of(Labels.QUARKUS_DEV_SERVICE), Arrays.stream(devServiceLabels))
                .map(l -> container.getLabels().get(l))
                .anyMatch(labelPredicate);
    }

    private static boolean hasMatchingPort(ContainerPort containerPort, int port) {
        return containerPort.getPrivatePort() != null &&
                containerPort.getPublicPort() != null &&
                containerPort.getPrivatePort().equals(port);
    }

    private final BiPredicate<Container, String> filter;
    private final int port;

    public ContainerLocator(String devServiceLabel, int port) {
        this((container, expectedLabel) -> expectedLabel.equals(container.getLabels().get(devServiceLabel)), port);
    }

    public ContainerLocator(BiPredicate<Container, String> filter, int port) {
        // Always tack on an extra filter to rule out containers which were clearly created by this process
        this.filter = filter.and(IS_NOT_CREATED_BY_US_SELECTOR);
        this.port = port;
    }

    public static ContainerLocator locateContainerWithLabels(int port, String... devServiceLabels) {
        return new ContainerLocator(
                (container, expectedLabel) -> hasDevServiceLabels(container, expectedLabel::equals, devServiceLabels),
                port);
    }

    private Stream<Container> lookup(String expectedLabelValue) {
        return DockerClientFactory.lazyClient().listContainersCmd().exec().stream()
                .filter(container -> filter.test(container, expectedLabelValue));
    }

    private Optional<ContainerPort> getMappedPort(Container container, int port) {
        return Arrays.stream(container.getPorts())
                .filter(containerPort -> hasMatchingPort(containerPort, port))
                .findAny();
    }

    public Optional<ContainerAddress> locateContainer(String serviceName, boolean shared, LaunchMode launchMode) {
        if (shared && launchMode == LaunchMode.DEVELOPMENT) {
            return lookup(serviceName)
                    .flatMap(container -> getMappedPort(container, port).stream()
                            .flatMap(containerPort -> Optional.ofNullable(containerPort.getPublicPort())
                                    .map(port -> {
                                        final ContainerAddress containerAddress = new ContainerAddress(
                                                container.getId(),
                                                DockerClientFactory.instance().dockerHostIpAddress(),
                                                containerPort.getPublicPort());
                                        log.infof("Dev Services container found: %s (%s). Connecting to: %s.",
                                                container.getId(),
                                                container.getImage(),
                                                containerAddress.getUrl());
                                        return containerAddress;
                                    }).stream()))
                    .findFirst();
        } else {
            return Optional.empty();
        }
    }

    /**
     * @return container id, if exists
     */
    public Optional<String> locateContainer(String serviceName, boolean shared, LaunchMode launchMode,
            BiConsumer<Integer, ContainerAddress> consumer) {
        if (shared && launchMode == LaunchMode.DEVELOPMENT) {
            return lookup(serviceName).findAny()
                    .map(container -> {
                        Arrays.stream(container.getPorts())
                                .filter(cp -> Objects.nonNull(cp.getPublicPort()) && Objects.nonNull(cp.getPrivatePort()))
                                .forEach(cp -> {
                                    ContainerAddress containerAddress = new ContainerAddress(
                                            container.getId(),
                                            DockerClientFactory.instance().dockerHostIpAddress(),
                                            cp.getPublicPort());
                                    consumer.accept(cp.getPrivatePort(), containerAddress);
                                });
                        return container.getId();
                    });
        } else {
            return Optional.empty();
        }
    }

    public Optional<Integer> locatePublicPort(String serviceName, boolean shared, LaunchMode launchMode, int privatePort) {
        if (shared && launchMode == LaunchMode.DEVELOPMENT) {
            return lookup(serviceName)
                    .flatMap(container -> getMappedPort(container, privatePort).stream())
                    .findFirst()
                    .map(ContainerPort::getPublicPort);
        } else {
            return Optional.empty();
        }
    }
}
