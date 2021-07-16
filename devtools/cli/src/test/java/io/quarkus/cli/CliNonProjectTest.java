package io.quarkus.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import picocli.CommandLine;

public class CliNonProjectTest {
    static Path workspaceRoot;

    @BeforeAll
    public static void initial() throws Exception {
        workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
                .resolve("target/test-project/CliNonProjectTest");
        CliDriver.deleteDir(workspaceRoot);
        Files.createDirectories(workspaceRoot);
    }

    @AfterEach
    public void verifyEmptyDirectory() throws Exception {
        String[] files = workspaceRoot.toFile().list();
        Assertions.assertNotNull(files,
                "Directory list operation should succeed");
        Assertions.assertEquals(0, files.length,
                "Directory should be empty. Found: " + Arrays.toString(files));
    }

    @Test
    public void testListOutsideOfProject() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("Jackson"),
                "Should contain 'Jackson' in the list of extensions, found: " + result.stdout);
    }

    @Test
    public void testListPlatformExtensions() throws Exception {
        // List extensions of a specified platform version
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "list", "-P=io.quarkus:quarkus-bom:2.0.0.CR3", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("Jackson"),
                "Should contain 'Jackson' in the list of extensions, found: " + result.stdout);
        Assertions.assertTrue(result.stdout.contains("2.0.0.CR3"),
                "Should contain '2.0.0.CR3' in the list of extensions (origin), found: " + result.stdout);
    }

    @Test
    public void testBuildOutsideOfProject() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "build", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.USAGE, result.exitCode,
                "'quarkus build' should fail outside of a quarkus project directory:\n" + result);
    }

    @Test
    public void testDevOutsideOfProject() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "dev", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.USAGE, result.exitCode,
                "'quarkus dev' should fail outside of a quarkus project directory:\n" + result);
    }

    @Test
    public void testCreateAppDryRun() throws Exception {
        // A dry run of create should not create any files or directories
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "--dry-run", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("project would have been created"),
                "Should contain 'project would have been created', found: " + result.stdout);

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "create", "--dryrun", "-e");
        Assertions.assertEquals(result.stdout, result2.stdout,
                "Invoking the command with --dryrun should produce the same result");
    }

    @Test
    public void testRegistryRefresh() throws Exception {
        // List extensions of a specified platform version
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "registry", "--refresh", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);
    }
}
