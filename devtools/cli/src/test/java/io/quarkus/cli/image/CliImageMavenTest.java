package io.quarkus.cli.image;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
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
            .resolve("target/test-project/CliImageMavenTest");
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
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);

        // 1 image --help
        result = CliDriver.execute(project, "image", "--help");
        Assertions.assertTrue(result.getStdout().contains("Commands:"), "Should list subcommands\n");
        Assertions.assertTrue(result.getStdout().contains("build"), "Should list build subcommand\n");
        Assertions.assertTrue(result.getStdout().contains("push"), "Should list build subcommand\n");

        // 2 image build --help
        result = CliDriver.execute(project, "image", "build", "--help");
        Assertions.assertTrue(result.getStdout().contains("--group"), "Should have --group option\n");
        Assertions.assertTrue(result.getStdout().contains("--name"), "Should have --name  option\n");
        Assertions.assertTrue(result.getStdout().contains("--tag"), "Should have --tag  option\n");
        Assertions.assertTrue(result.getStdout().contains("Commands:"), "Should list subcommands\n");
        Assertions.assertTrue(result.getStdout().contains("docker"), "Should list docker subcommand\n");
        Assertions.assertTrue(result.getStdout().contains("jib"), "Should list jib subcommand\n");
        Assertions.assertTrue(result.getStdout().contains("openshift"), "Should list openshift subcommand\n");
        Assertions.assertTrue(result.getStdout().contains("buildpack"), "Should list buildpack subcommand\n");

        // 3 image push --help
        result = CliDriver.execute(project, "image", "push", "--help");
        Assertions.assertTrue(result.getStdout().contains("--group"), "Should have --group option\n");
        Assertions.assertTrue(result.getStdout().contains("--name"), "Should have --name  option\n");
        Assertions.assertTrue(result.getStdout().contains("--tag"), "Should have --tag  option\n");
    }
}
