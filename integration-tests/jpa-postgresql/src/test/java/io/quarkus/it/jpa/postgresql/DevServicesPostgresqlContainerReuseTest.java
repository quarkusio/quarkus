package io.quarkus.it.jpa.postgresql;

import static io.quarkus.it.jpa.postgresql.DevServicesPostgresqFixedPortTest.findContainerOnPort;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import io.quarkus.maven.it.MojoTestBase;
import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

/**
 * Reproducer for <a href="https://github.com/quarkusio/quarkus/issues/53312">#53312</a>.
 * <p>
 * Runs a Quarkus application that uses DevServices PostgreSQL with a fixed port and
 * {@code testcontainers.reuse.enable=true} twice in sequence. The second run should
 * reuse the container from the first run. If it cannot (because the process-uuid label
 * changed), the second run fails with a port conflict.
 * <p>
 */
class DevServicesPostgresqlContainerReuseTest extends MojoTestBase {

    private static final int FIXED_PORT = 55432;

    @BeforeAll
    static void ensurePortIsClear() {
        String containerId = findContainerOnPort(FIXED_PORT);
        if (containerId != null) {
            DockerClientFactory.lazyClient().removeContainerCmd(containerId).withForce(true).exec();
        }
    }

    @AfterAll
    static void removeContainers() {
        String containerId = findContainerOnPort(FIXED_PORT);
        if (containerId != null) {
            DockerClientFactory.lazyClient().removeContainerCmd(containerId).withForce(true).exec();
        }
    }

    @Test
    void testContainerReusedAcrossRuns() throws Exception {
        List<String> goals = List.of("clean", "test", "-Dquarkus.analytics.disabled=true");
        // DevServicesBuildTimeConfig.reuse() defaults to true, so the PostgreSQL
        // container is created with .withReuse(true). This env var is the
        // testcontainers-side switch that must also be enabled for reuse to work.
        Map<String, String> envVars = Map.of("TESTCONTAINERS_REUSE_ENABLE", "true");
        File testDir = initProject("projects/devservices-postgresql-reuse",
                "projects/devservices-postgresql-reuse-run");

        // First run — should start a container on port 55432 and pass
        RunningInvoker firstRun = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult firstResult = firstRun.execute(goals, envVars);
        assertThat(firstResult.getProcess().waitFor())
                .as("First run should succeed")
                .isZero();
        firstRun.stop();

        String firstContainerId = findContainerOnPort(FIXED_PORT);
        assertThat(firstContainerId)
                .as("A container should be running on port %d after the first run", FIXED_PORT)
                .isNotNull();

        // Second run — should reuse the container from the first run.
        // If the process-uuid label prevents reuse, this fails with a port conflict
        // because the first container is still running on port 55432.
        RunningInvoker secondRun = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult secondResult = secondRun.execute(goals, envVars);
        assertThat(secondResult.getProcess().waitFor())
                .as("Second run should succeed by reusing the container (see #53312)")
                .isZero();
        secondRun.stop();

        String secondContainerId = findContainerOnPort(FIXED_PORT);
        assertThat(secondContainerId)
                .as("The same container should be reused across runs (see #53312)")
                .isEqualTo(firstContainerId);
    }

}
