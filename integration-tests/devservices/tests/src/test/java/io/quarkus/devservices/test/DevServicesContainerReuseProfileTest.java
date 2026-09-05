package io.quarkus.devservices.test;

import static io.quarkus.devservices.test.DevServicesContainerReuseTest.findContainerOnPort;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import io.quarkus.maven.it.MojoTestBase;
import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

/**
 * Verifies that DevServices correctly reuses or replaces containers across
 * test classes with same, compatible, or incompatible profiles, when
 * Testcontainers reuse is enabled.
 * <p>
 * Covers gaps from the dev services lifecycle table
 * (<a href="https://github.com/quarkusio/quarkus/issues/55881">#55881</a>):
 * <ul>
 * <li>Reuse=Yes, same config profile → container should survive</li>
 * <li>Reuse=Yes, compatible different profile → container should survive
 * (not yet implemented, see <a href="https://github.com/quarkusio/quarkus/issues/53114">#53114</a>)</li>
 * <li>Reuse=Yes, incompatible profile → container should be killed</li>
 * </ul>
 * <p>
 * Uses a template project with four ordered test classes that each record
 * their container ID to a file. This test runs them with
 * {@code TESTCONTAINERS_REUSE_ENABLE=true} and then compares the IDs.
 */
class DevServicesContainerReuseProfileTest extends MojoTestBase {

    private static final int FIXED_PORT = 57432;
    private static final int INCOMPATIBLE_PORT = 57433;

    private static volatile RunResult cachedResult;

    @BeforeAll
    static void ensurePortsAreClear() {
        removeContainerOnPort(FIXED_PORT);
        removeContainerOnPort(INCOMPATIBLE_PORT);
    }

    @AfterAll
    static void removeContainers() {
        removeContainerOnPort(FIXED_PORT);
        removeContainerOnPort(INCOMPATIBLE_PORT);
    }

    @Test
    void testContainerSurvivesSameProfileWithReuse() throws Exception {
        RunResult r = ensureProjectRan();

        assertThat(r.sameProfileId)
                .as("Container should survive between tests with the same profile (reuse=yes, see #55881)")
                .isEqualTo(r.firstId);
    }

    // Cross-profile container reuse is not yet implemented: a profile change
    // always kills the container because the application instance UUID changes.
    // See https://github.com/quarkusio/quarkus/issues/53114
    @Disabled("Cross-profile container reuse is not yet implemented, see #53114")
    @Test
    void testContainerSurvivesCompatibleDifferentProfileWithReuse() throws Exception {
        RunResult r = ensureProjectRan();

        assertThat(r.compatibleProfileId)
                .as("Container should survive when profile changes but container config is compatible"
                        + " (reuse=yes, see #53114)")
                .isEqualTo(r.firstId);
    }

    @Test
    void testContainerReplacedOnIncompatibleProfileWithReuse() throws Exception {
        RunResult r = ensureProjectRan();

        assertThat(r.incompatibleProfileId)
                .as("Container should be replaced when profile changes with incompatible config"
                        + " (reuse=yes, see #55881)")
                .isNotEqualTo(r.firstId);
    }

    private synchronized RunResult ensureProjectRan() throws Exception {
        if (cachedResult != null) {
            return cachedResult;
        }

        List<String> goals = List.of("clean", "test", "-Dquarkus.analytics.disabled=true");
        Map<String, String> envVars = Map.of("TESTCONTAINERS_REUSE_ENABLE", "true");
        File testDir = initProject("projects/devservices-container-reuse-profile-test",
                "projects/devservices-container-reuse-profile-test-run");

        RunningInvoker run = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = run.execute(goals, envVars);
        assertThat(result.getProcess().waitFor())
                .as("Template project build should succeed")
                .isZero();
        run.stop();

        cachedResult = new RunResult(
                readContainerId(testDir, "container-id-first.txt"),
                readContainerId(testDir, "container-id-same-profile.txt"),
                readContainerId(testDir, "container-id-compatible-profile.txt"),
                readContainerId(testDir, "container-id-incompatible-profile.txt"));
        return cachedResult;
    }

    private static String readContainerId(File testDir, String filename) throws Exception {
        return Files.readString(testDir.toPath().resolve("target/" + filename)).trim();
    }

    private static void removeContainerOnPort(int port) {
        String containerId = findContainerOnPort(port);
        if (containerId != null) {
            DockerClientFactory.lazyClient().removeContainerCmd(containerId).withForce(true).exec();
        }
    }

    private record RunResult(String firstId, String sameProfileId, String compatibleProfileId,
            String incompatibleProfileId) {
    }
}
