package io.quarkus.devservices.test;

import static io.quarkus.devservices.test.DevServicesContainerReuseTest.findContainerOnPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import io.quarkus.maven.it.MojoTestBase;
import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

/**
 * Verifies that DevServices does NOT reuse containers across runs
 * when Testcontainers reuse is not explicitly enabled.
 * <p>
 * This is the counterpart of {@link DevServicesContainerReuseTest}:
 * that test enables {@code TESTCONTAINERS_REUSE_ENABLE=true} and asserts reuse works;
 * this test uses default settings and asserts reuse does NOT happen.
 * <p>
 * Uses a fixed port so that a lingering (not-stopped) container from the first run
 * would cause a port conflict in the second run, catching the bug.
 */
class DevServicesContainerNoReuseTest extends MojoTestBase {

    private static final int FIXED_PORT = 56433;
    private static final File TESTCONTAINERS_PROPS_FILE = new File(
            System.getProperty("user.home"), ".testcontainers.properties");
    private static final File TESTCONTAINERS_PROPS_BACKUP = new File(System.getProperty("user.home"),
            ".testcontainers.properties.backup");

    @BeforeAll
    static void setUp() throws IOException {
        String containerId = findContainerOnPort(FIXED_PORT);
        if (containerId != null) {
            DockerClientFactory.lazyClient().removeContainerCmd(containerId).withForce(true).exec();
        }

        /* Backup existing .testcontainers.properties if present */
        if (TESTCONTAINERS_PROPS_FILE.exists()) {
            Files.copy(TESTCONTAINERS_PROPS_FILE.toPath(), TESTCONTAINERS_PROPS_BACKUP.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

        }
    }

    @AfterAll
    static void tearDown() throws IOException {
        String containerId = findContainerOnPort(FIXED_PORT);
        if (containerId != null) {
            DockerClientFactory.lazyClient().removeContainerCmd(containerId).withForce(true).exec();
        }
        // Restore original .testcontainers.properties
        if (TESTCONTAINERS_PROPS_BACKUP.exists()) {
            Files.move(TESTCONTAINERS_PROPS_BACKUP.toPath(), TESTCONTAINERS_PROPS_FILE.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } else if (TESTCONTAINERS_PROPS_FILE.exists()) {
            // If there was no backup but file now exists, remove it (test created it)
            TESTCONTAINERS_PROPS_FILE.delete();
        }
    }

    @Test
    void testContainerEventuallyStoppedAfterQuarkusTest() throws Exception {
        List<String> goals = List.of("clean", "test", "-Dquarkus.analytics.disabled=true");
        Map<String, String> envVars = Map.of("TESTCONTAINERS_REUSE_ENABLE", "false");
        File testDir = initProject("projects/devservices-container-no-reuse",
                "projects/devservices-container-no-reuse-run");

        RunningInvoker firstRun = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult firstResult = firstRun.execute(goals, envVars);
        assertThat(firstResult.getProcess().waitFor())
                .as("First run should succeed")
                .isZero();
        firstRun.stop();

        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(findContainerOnPort(FIXED_PORT))
                        .as("Container on port %d should be stopped after the first run"
                                + " (reuse is not enabled, so ContainerShutdownCloseable must call container.stop())",
                                FIXED_PORT)
                        .isNull());

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

        RunningInvoker secondRun = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult secondResult = secondRun.execute(goals, envVars);
        assertThat(secondResult.getProcess().waitFor())
                .as("Second run should succeed — the first container must have been stopped,"
                        + " freeing port %d for a fresh container",
                        FIXED_PORT)
                .isZero();
        secondRun.stop();
    }

    /*
     * This is a stronger version of testContainerEventuallyStoppedAfterQuarkusTest, which does not rely on Ryuk.
     */
    @Disabled("Not yet working, see https://github.com/quarkusio/quarkus/issues/55605")
    @Test
    void testContainerStoppedImmediatelyAfterQuarkusTest() throws Exception {
        List<String> goals = List.of("clean", "test", "-Dquarkus.analytics.disabled=true");
        // Explicitly disable reuse - containers should be stopped after test
        Map<String, String> envVars = Map.of("TESTCONTAINERS_REUSE_ENABLE", "false");
        File testDir = initProject("projects/devservices-container-stop-after-test",
                "projects/devservices-container-stop-after-test-run");

        // First run - starts a DevServices container on the fixed port
        RunningInvoker firstRun = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult firstResult = firstRun.execute(goals, envVars);
        assertThat(firstResult.getProcess().waitFor())
                .as("First run should succeed")
                .isZero();
        firstRun.stop();

        assertThat(findContainerOnPort(FIXED_PORT))
                .as("Container on port %d should be stopped after the first run"
                        + " (reuse is not enabled, so the container must be stopped on shutdown)",
                        FIXED_PORT)
                .isNull();

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

        // Second run - should succeed because the first container was stopped,
        // freeing the fixed port. If the first container were still running,
        // this would fail with a port conflict.
        RunningInvoker secondRun = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult secondResult = secondRun.execute(goals, envVars);
        assertThat(secondResult.getProcess().waitFor())
                .as("Second run should succeed - the first container must have been stopped,"
                        + " freeing port %d for a fresh container",
                        FIXED_PORT)
                .isZero();
        secondRun.stop();
    }
}
