package io.quarkus.it.jpa.postgresql;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;

import io.quarkus.maven.it.MojoTestBase;
import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

class DevServicesPostgresqFixedPortTest extends MojoTestBase {

    private static final int FIXED_PORT = 55434;

    @BeforeAll
    static void checkPortIsClear() {
        assertThat(findContainerOnPort(FIXED_PORT))
                .as("Port %d must not be in use before the test starts", FIXED_PORT)
                .isNull();
    }

    @AfterAll
    static void removeContainers() {
        String containerId = findContainerOnPort(FIXED_PORT);
        if (containerId != null) {
            DockerClientFactory.lazyClient().removeContainerCmd(containerId).withForce(true).exec();
        }
    }

    @Test
    void testContainerFixedPort() throws Exception {
        List<String> goals = List.of("clean", "test", "-Dquarkus.analytics.disabled=true");

        Map<String, String> envVars = Map.of();
        File testDir = initProject("projects/devservices-postgresql-fixed-port",
                "projects/devservices-postgresql-fixed-port-run");

        // Should start a container on port 55434 and pass
        RunningInvoker run = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult firstResult = run.execute(goals, envVars);
        assertThat(firstResult.getProcess().waitFor())
                .as("Launch should succeed")
                .isZero();
        run.stop();

        String firstContainerId = findContainerOnPort(FIXED_PORT);
        assertThat(firstContainerId)
                .as("A container should be running on port %d after the test is run", FIXED_PORT)
                .isNotNull();
    }

    static String findContainerOnPort(int publicPort) {
        return DockerClientFactory.lazyClient().listContainersCmd().exec().stream()
                .filter(container -> hasPublicPort(container, publicPort))
                .map(Container::getId)
                .findFirst()
                .orElse(null);
    }

    static boolean hasPublicPort(Container container, int publicPort) {
        if (container.getPorts() == null) {
            return false;
        }
        return Arrays.stream(container.getPorts())
                .map(ContainerPort::getPublicPort)
                .anyMatch(p -> p != null && p == publicPort);
    }
}
