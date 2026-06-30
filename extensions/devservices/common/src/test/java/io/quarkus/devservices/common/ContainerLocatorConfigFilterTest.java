package io.quarkus.devservices.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.runtime.LaunchMode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContainerLocatorConfigFilterTest {

    private static final String TEST_LABEL = "io.quarkus.devservice.test";
    private static final String SERVICE_NAME = "config-filter-test";
    private static final int CONTAINER_PORT = 6379;
    private static final int FIXED_PORT_A = 16371;

    private GenericContainer<?> containerWithPortLabel;
    private GenericContainer<?> containerWithoutPortLabel;

    @BeforeAll
    void startContainers() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable());

        try {
            containerWithPortLabel = new FixedPortContainer(DockerImageName.parse("redis:8.8"), FIXED_PORT_A,
                    CONTAINER_PORT)
                    .withLabel(TEST_LABEL, SERVICE_NAME)
                    .withLabel(Labels.QUARKUS_DEV_SERVICE_CONFIG + "port", String.valueOf(FIXED_PORT_A));
            containerWithPortLabel.start();

            containerWithoutPortLabel = new GenericContainer<>(DockerImageName.parse("redis:8.8"))
                    .withLabel(TEST_LABEL, SERVICE_NAME)
                    .withExposedPorts(CONTAINER_PORT);
            containerWithoutPortLabel.start();

        } catch (Exception e) {
            stopContainers();
        }
    }

    private static class FixedPortContainer extends GenericContainer<FixedPortContainer> {
        FixedPortContainer(DockerImageName image, int hostPort, int containerPort) {
            super(image);
            addFixedExposedPort(hostPort, containerPort);
        }
    }

    @AfterAll
    void stopContainers() {
        if (containerWithPortLabel != null) {
            containerWithPortLabel.stop();
        }
        if (containerWithoutPortLabel != null) {
            containerWithoutPortLabel.stop();
        }
    }

    @Test
    void emptyExpectedConfigMatchesAnyContainer() {
        ContainerLocator locator = new ContainerLocator(TEST_LABEL, CONTAINER_PORT);
        Optional<ContainerAddress> result = locator.locateContainer(SERVICE_NAME, true, LaunchMode.DEVELOPMENT, Map.of());
        assertThat(result).isPresent();
    }

    @Test
    void matchingPortConfigFindsContainer() {
        ContainerLocator locator = new ContainerLocator(TEST_LABEL, CONTAINER_PORT);
        Optional<ContainerAddress> result = locator.locateContainer(SERVICE_NAME, true, LaunchMode.DEVELOPMENT,
                Labels.expectedPortConfig(OptionalInt.of(FIXED_PORT_A)));
        assertThat(result).isPresent();
        assertThat(result.get().getPort()).isEqualTo(FIXED_PORT_A);
    }

    @Test
    void mismatchedPortConfigSkipsContainer() {
        ContainerLocator locator = new ContainerLocator(TEST_LABEL, CONTAINER_PORT);
        Optional<ContainerAddress> result = locator.locateContainer(SERVICE_NAME, true, LaunchMode.DEVELOPMENT,
                Labels.expectedPortConfig(OptionalInt.of(FIXED_PORT_A + 100)));
        assertThat(result).isEmpty();
    }

    @Test
    void containerWithoutLabelNotFoundByPortFilter() {
        ContainerLocator locator = new ContainerLocator(TEST_LABEL, CONTAINER_PORT);
        int randomPort = containerWithoutPortLabel.getMappedPort(CONTAINER_PORT);
        Optional<ContainerAddress> result = locator.locateContainer(SERVICE_NAME, true, LaunchMode.DEVELOPMENT,
                Labels.expectedPortConfig(OptionalInt.of(randomPort)));
        assertThat(result).isEmpty();
    }

    @Test
    void noConfigFilterStillFindsContainerWithLabel() {
        ContainerLocator locator = new ContainerLocator(TEST_LABEL, CONTAINER_PORT);
        Optional<ContainerAddress> result = locator.locateContainer(SERVICE_NAME, true, LaunchMode.DEVELOPMENT);
        assertThat(result).isPresent();
    }
}
