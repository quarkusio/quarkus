package io.quarkus.it.jpa.postgresql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;

import io.quarkus.maven.it.MojoTestBase;
import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.test.devmode.util.DevModeClient;

class DevServicesPostgresqFixedPortTest extends MojoTestBase {

    private static final int FIXED_PORT = 55434;

    private RunningInvoker run;
    private DevModeClient devModeClient;

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

    @AfterEach
    void cleanup() {
        if (run != null) {
            run.stop();
        }
        if (devModeClient != null) {
            devModeClient.awaitUntilServerDown();
        }
        String containerId = findContainerOnPort(FIXED_PORT);
        if (containerId != null) {
            DockerClientFactory.lazyClient().removeContainerCmd(containerId).withForce(true).exec();
        }
    }

    @Test
    void testContainerFixedPortInDevMode() throws Exception {
        File testDir = initProject("projects/devservices-postgresql-fixed-port",
                "projects/devservices-postgresql-fixed-port-dev");

        run = new RunningInvoker(testDir, false);
        run.execute(List.of("compile", "quarkus:dev",
                "-Dquarkus.analytics.disabled=true", "-Ddebug=false",
                "-Dquarkus.enforceBuildGoal=false",
                "-Djvm.args=-Xmx128m"), Map.of());

        devModeClient = new DevModeClient(8084);
        devModeClient.getHttpResponse("/fruits");

        String containerId = findContainerOnPort(FIXED_PORT);
        assertThat(containerId)
                .as("A container should be running on port %d while the app is in dev mode", FIXED_PORT)
                .isNotNull();
    }

    @Test
    void testContainerFixedPortInTestMode() throws Exception {
        File testDir = initProject("projects/devservices-postgresql-fixed-port",
                "projects/devservices-postgresql-fixed-port-test");

        run = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = run.execute(
                List.of("clean", "test", "-Dquarkus.analytics.disabled=true"), Map.of());

        await().atMost(3, TimeUnit.MINUTES)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(findContainerOnPort(FIXED_PORT))
                        .as("A container should be running on port %d while the test is executing", FIXED_PORT)
                        .isNotNull());

        assertThat(result.getProcess().waitFor())
                .as("Test run should succeed")
                .isZero();
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
