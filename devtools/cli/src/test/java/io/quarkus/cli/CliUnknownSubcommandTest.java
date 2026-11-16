package io.quarkus.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.RegistryClientTestHelper;
import picocli.CommandLine;

/**
 * A mistyped subcommand of a command that forwards to a default subcommand ({@code create}, {@code image},
 * {@code extension}, {@code plugin}) is reported as an unknown subcommand with a suggestion, instead of the
 * unmatched-argument error of the default subcommand.
 */
public class CliUnknownSubcommandTest {

    static Path workspaceRoot;

    @BeforeAll
    public static void initial() throws Exception {
        workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
                .resolve("target/test-classes/test-project/CliUnknownSubcommandTest");
        CliDriver.deleteDir(workspaceRoot);
        Files.createDirectories(workspaceRoot);
    }

    @BeforeEach
    public void setupTestRegistry() {
        RegistryClientTestHelper.reenableRegistryClientTestConfig();
    }

    @Test
    public void testCreateWithMistypedSubcommand() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "ext", "abc", "-e");
        assertUnknownSubcommand(result, "ext", "extension");
        Assertions.assertFalse(result.stdout.contains("Creating an app"), result.toString());
        Assertions.assertEquals(0, workspaceRoot.toFile().list().length, "No project must be created");
    }

    @Test
    public void testCreateWithProjectNameStillForwards() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "my-project", "--dryrun", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, result.toString());
        Assertions.assertTrue(result.stdout.contains("Creating an app"), result.toString());
    }

    @Test
    public void testExtensionWithMistypedSubcommand() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "lst", "-e");
        assertUnknownSubcommand(result, "lst", "list");
    }

    @Test
    public void testPluginWithMistypedSubcommand() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "plug", "lsit", "-e");
        assertUnknownSubcommand(result, "lsit", "list");
    }

    @Test
    public void testImageWithMistypedSubcommand() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "biuld", "-e");
        assertUnknownSubcommand(result, "biuld", "build");
    }

    private static void assertUnknownSubcommand(CliDriver.Result result, String typo, String suggestion) {
        Assertions.assertEquals(CommandLine.ExitCode.USAGE, result.exitCode, result.toString());
        String output = result.stdout + result.stderr;
        Assertions.assertTrue(output.contains("Unknown subcommand '" + typo + "'"), result.toString());
        Assertions.assertTrue(output.contains("Did you mean") && output.contains(suggestion), result.toString());
        Assertions.assertFalse(output.contains("Unmatched argument"), result.toString());
    }
}
