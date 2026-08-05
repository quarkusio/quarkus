package io.quarkus.it.jpa.postgresql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

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
 * Verifies that DevServices PostgreSQL does NOT reuse containers across runs
 * when Testcontainers reuse is not explicitly enabled.
 * <p>
 * This is the counterpart of {@link DevServicesPostgresqlContainerReuseTest}:
 * that test enables {@code TESTCONTAINERS_REUSE_ENABLE=true} and asserts reuse works;
 * this test uses default settings and asserts reuse does NOT happen.
 * <p>
 * Uses a fixed port so that a lingering (not-stopped) container from the first run
 * would cause a port conflict in the second run, catching the bug.
 */
class DevServicesPostgresqlContainerNoReuseTest extends MojoTestBase {

    private static final int FIXED_PORT = 55433;
    private static final File TESTCONTAINERS_PROPS_FILE = new File(
            System.getProperty("user.home"), ".testcontainers.properties");

    @BeforeAll
    static void setUp() {
        String containerId = findContainerOnPort(FIXED_PORT);
        if (containerId != null) {
            DockerClientFactory.lazyClient().removeContainerCmd(containerId).withForce(true).exec();
        }
    }

    @AfterAll
    static void tearDown() {
        String containerId = findContainerOnPort(FIXED_PORT);
        if (containerId != null) {
            DockerClientFactory.lazyClient().removeContainerCmd(containerId).withForce(true).exec();
        }
    }

    @Test
    void testContainerNotReusedWithDefaultSettings() throws Exception {
        List<String> goals = List.of("clean", "test", "-Dquarkus.analytics.disabled=true");
        // DevServicesBuildTimeConfig.reuse() defaults to true, so the PostgreSQL
        // container is created with .withReuse(true). This env var is the
        // testcontainers-side switch that must also be enabled for reuse to work.
        // It's disabled by default, but since we can't be sure of the environment,
        // we force it here.
        Map<String, String> envVars = Map.of("TESTCONTAINERS_REUSE_ENABLE", "false");
        File testDir = initProject("projects/devservices-postgresql-reuse",
                "projects/devservices-postgresql-reuse-run");

        // First run — starts a DevServices PostgreSQL container on the fixed port
        RunningInvoker firstRun = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult firstResult = firstRun.execute(goals, envVars);
        assertThat(firstResult.getProcess().waitFor())
                .as("First run should succeed")
                .isZero();
        firstRun.stop();

        // Container shutdown may not be instantaneous: for some reason,
        // Quarkus doesn't shut down containers itself but lets Ryuk take care of it.
        // See https://quarkusio.zulipchat.com/#narrow/channel/187038-dev/topic/Hibernate.20test.20failures.20on.20CI/near/608660701
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(findContainerOnPort(FIXED_PORT))
                        .as("Container on port %d should be stopped after the first run"
                                + " (reuse is not enabled, so ContainerShutdownCloseable must call container.stop())",
                                FIXED_PORT)
                        .isNull());

        // IsContainerRuntimeWorking must not persist testcontainers.reuse.enable to disk
        if (TESTCONTAINERS_PROPS_FILE.exists()) {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(TESTCONTAINERS_PROPS_FILE)) {
                props.load(in);
            }
            assertThat(props.getProperty("testcontainers.reuse.enable"))
                    .as("~/.testcontainers.properties must not contain testcontainers.reuse.enable"
                            + " (IsContainerRuntimeWorking should only modify this setting in-memory)")
                    .isNull();
        }

        // Second run — should succeed because the first container was stopped,
        // freeing the fixed port. If the first container were still running,
        // this would fail with a port conflict.
        RunningInvoker secondRun = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult secondResult = secondRun.execute(goals, envVars);
        assertThat(secondResult.getProcess().waitFor())
                .as("Second run should succeed — the first container must have been stopped,"
                        + " freeing port %d for a fresh container",
                        FIXED_PORT)
                .isZero();
        secondRun.stop();
    }

    private static String findContainerOnPort(int publicPort) {
        return DockerClientFactory.lazyClient().listContainersCmd().exec().stream()
                .filter(container -> hasPublicPort(container, publicPort))
                .map(Container::getId)
                .findFirst()
                .orElse(null);
    }

    private static boolean hasPublicPort(Container container, int publicPort) {
        if (container.getPorts() == null) {
            return false;
        }
        return Arrays.stream(container.getPorts())
                .map(ContainerPort::getPublicPort)
                .anyMatch(p -> p != null && p == publicPort);
    }
}
