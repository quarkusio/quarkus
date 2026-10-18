package io.quarkus.devservices.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
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

/**
 * Reproducer for <a href="https://github.com/quarkusio/quarkus/issues/53312">#53312</a>.
 * <p>
 * Runs a Quarkus application that uses DevServices with a fixed port and
 * {@code testcontainers.reuse.enable=true} twice in sequence. The second run should
 * reuse the container from the first run. If it cannot (because the process-uuid label
 * changed), the second run fails with a port conflict.
 * <p>
 */
class DevServicesContainerReuseTest extends MojoTestBase {

    /**
     * A free port chosen at runtime to pin the dev service, so both runs target the
     * same port. A hardcoded port is racy: a leftover host-level listener (for
     * example a stale docker-proxy) is not released by the container-based cleanup
     * and causes "address already in use" failures (see #57045).
     */
    private static int fixedPort;

    @BeforeAll
    static void choosePort() {
        fixedPort = findAvailablePort();
    }

    @AfterAll
    static void removeContainers() {
        String containerId = findContainerOnPort(fixedPort);
        if (containerId != null) {
            DockerClientFactory.lazyClient().removeContainerCmd(containerId).withForce(true).exec();
        }
    }

    @Test
    void testContainerReusedAcrossRuns() throws Exception {
        List<String> goals = List.of("clean", "test", "-Dquarkus.analytics.disabled=true");
        Map<String, String> envVars = Map.of(
                "TESTCONTAINERS_REUSE_ENABLE", "true",
                "QUARKUS_SIMPLE_EXTENSION_DEVSERVICES_PORT", Integer.toString(fixedPort));
        File testDir = initProject("projects/devservices-container-reuse",
                "projects/devservices-container-reuse-run");

        RunningInvoker firstRun = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult firstResult = firstRun.execute(goals, envVars);
        assertThat(firstResult.getProcess().waitFor())
                .as("First run should succeed")
                .isZero();
        firstRun.stop();

        String firstContainerId = findContainerOnPort(fixedPort);
        assertThat(firstContainerId)
                .as("A container should be running on port %d after the first run", fixedPort)
                .isNotNull();

        RunningInvoker secondRun = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult secondResult = secondRun.execute(goals, envVars);
        assertThat(secondResult.getProcess().waitFor())
                .as("Second run should succeed by reusing the container (see #53312)")
                .isZero();
        secondRun.stop();

        String secondContainerId = findContainerOnPort(fixedPort);
        assertThat(secondContainerId)
                .as("The same container should be reused across runs (see #53312)")
                .isEqualTo(firstContainerId);
    }

    private static int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to find a free port", e);
        }
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
