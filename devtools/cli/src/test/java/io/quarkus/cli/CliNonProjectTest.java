package io.quarkus.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import picocli.CommandLine;

public class CliNonProjectTest {
    static String startingDir;
    static Path workspaceRoot;

    @BeforeAll
    public static void initial() throws Exception {
        startingDir = System.getProperty("user.dir");
        workspaceRoot = Paths.get(startingDir).toAbsolutePath().resolve("target/test-project/CliNonProjectTest");
        System.setProperty("user.dir", workspaceRoot.toFile().getAbsolutePath());
        CliDriver.deleteDir(workspaceRoot);
        Files.createDirectories(workspaceRoot);
    }

    @AfterEach
    public void verifyEmptyDirectory() throws Exception {
        System.setProperty("user.dir", workspaceRoot.toFile().getAbsolutePath());
        String[] files = workspaceRoot.toFile().list();
        Assertions.assertNotNull(files,
                "Directory list operation should succeed");
        Assertions.assertEquals(0, files.length,
                "Directory should be empty. Found: " + Arrays.toString(files));
    }

    @AfterAll
    public static void allDone() {
        System.setProperty("user.dir", startingDir);
    }

    @Test
    public void testListOutsideOfProject() throws Exception {
        CliDriver.Result result = CliDriver.execute("ext", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.USAGE, result.exitCode,
                "'quarkus ext list' should fail outside of a quarkus project directory:\n" + result);
        System.out.println(result);
    }

    @Test
    public void testBuildOutsideOfProject() throws Exception {
        CliDriver.Result result = CliDriver.execute("build", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.USAGE, result.exitCode,
                "'quarkus build' should fail outside of a quarkus project directory:\n" + result);
        System.out.println(result);
    }

    @Test
    public void testDevOutsideOfProject() throws Exception {
        CliDriver.Result result = CliDriver.execute("dev", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.USAGE, result.exitCode,
                "'quarkus dev' should fail outside of a quarkus project directory:\n" + result);
        System.out.println(result);
    }

    @Test
    public void testCreateAppDryRun() throws Exception {
        // A dry run of create should not create any files or directories
        CliDriver.Result result = CliDriver.execute("create", "--dryrun", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("project would have been created"),
                "Should contain 'project would have been created', found: " + result.stdout);
    }

    @Test
    public void testCreateAppDryRunMistype() throws Exception {
        // A dry run of create should not create any files or directories
        CliDriver.Result result = CliDriver.execute("create", "--dry-run", "-e", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.USAGE, result.exitCode,
                "Expected OK return code." + result);
    }

    @Test
    public void testCreateCliDryRun() throws Exception {
        // A dry run of create should not create any files or directories
        CliDriver.Result result = CliDriver.execute("create", "cli", "--dryrun", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("project would have been created"),
                "Should contain 'project would have been created', found: " + result.stdout);
    }
}
