package io.quarkus.cli.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.cli.CliDriver;
import io.quarkus.devtools.testing.RegistryClientTestHelper;
import picocli.CommandLine;

/**
 * Similar to CliProjecMavenTest ..
 */
public class CliImageMavenTest {
    static Path workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("target/test-classes/test-project/CliImageMavenTest");
    Path project;

    @BeforeAll
    public static void setupTestRegistry() {
        RegistryClientTestHelper.enableRegistryClientTestConfig();
    }

    @AfterAll
    public static void cleanupTestRegistry() {
        RegistryClientTestHelper.disableRegistryClientTestConfig();
    }

    @BeforeEach
    public void setupTestDirectories() throws Exception {
        CliDriver.deleteDir(workspaceRoot);
        project = workspaceRoot.resolve("code-with-quarkus");
    }

    @Test
    public void testUsage() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "-e", "-B", "--verbose");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);

        // 1 image --dry-run
        result = CliDriver.execute(project, "image", "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        assertTrue(result.getStdout().contains("quarkus:image-build"));
        assertFalse(result.getStdout().contains("-Dnative"));
        result = CliDriver.execute(project, "image", "--native", "--dry-run");
        assertTrue(result.getStdout().contains("-Dnative"));

        // 2 image build --dry-run
        result = CliDriver.execute(project, "image", "build", "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        result = CliDriver.execute(project, "image", "build", "--group=mygroup", "--name=myname", "--tag=1.0", "--native",
                "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        assertTrue(result.getStdout().contains("-Dquarkus.container-image.group=mygroup"));
        assertTrue(result.getStdout().contains("-Dquarkus.container-image.name=myname"));
        assertTrue(result.getStdout().contains("-Dquarkus.container-image.tag=1.0"));
        assertTrue(result.getStdout().contains("-Dnative"));

        // 3 image push --dry-run
        result = CliDriver.execute(project, "image", "push", "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        assertTrue(result.getStdout().contains("-Dquarkus.container-image.build=false"));

        // 4 image push --also-build --dry-run --registry=quay.io
        result = CliDriver.execute(project, "image", "push", "--also-build", "--dry-run", "--registry=quay.io");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        assertTrue(result.getStdout().contains("-Dquarkus.container-image.build=true"));
        assertTrue(result.getStdout().contains("-Dquarkus.container-image.registry=quay.io"));

    }
}
