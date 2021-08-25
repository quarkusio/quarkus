package io.quarkus.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.testing.RegistryClientTestHelper;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigLocator;
import io.quarkus.registry.config.json.JsonRegistriesConfig;
import io.quarkus.registry.config.json.RegistriesConfigMapperHelper;
import picocli.CommandLine;

public class CliNonProjectTest {
    private static final String TEST_QUARKUS_REGISTRY = "test.quarkus.registry";
    static Path workspaceRoot;

    @BeforeAll
    public static void initial() throws Exception {
        workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
                .resolve("target/test-project/CliNonProjectTest");
        CliDriver.deleteDir(workspaceRoot);
        Files.createDirectories(workspaceRoot);
    }

    @BeforeEach
    public void setupTestRegistry() {
        RegistryClientTestHelper.reenableRegistryClientTestConfig();
    }

    @AfterEach
    public void verifyEmptyDirectory() throws Exception {
        String[] files = workspaceRoot.toFile().list();
        Assertions.assertNotNull(files,
                "Directory list operation should succeed");
        Assertions.assertEquals(0, files.length,
                "Directory should be empty. Found: " + Arrays.toString(files));
        RegistryClientTestHelper.disableRegistryClientTestConfig();
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
    public void testListPlatformExtensionsRegistryClient() throws Exception {
        // Dry run: Make sure registry-client system property is true
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "list", "-e",
                "--dry-run", "--registry-client");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);

        String noSpaces = result.stdout.replaceAll("[\\s\\p{Z}]", "");
        Assertions.assertTrue(noSpaces.contains("RegistryClienttrue"),
                "Should contain 'Registry Client true', found: " + result.stdout);
        Assertions.assertTrue(Boolean.getBoolean("quarkusRegistryClient"),
                "Registry Client property should be set to true");

        // Dry run: Make sure registry-client system property is false
        result = CliDriver.execute(workspaceRoot, "ext", "list", "-e",
                "--dry-run", "--no-registry-client");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);

        noSpaces = result.stdout.replaceAll("[\\s\\p{Z}]", "");
        Assertions.assertTrue(noSpaces.contains("RegistryClientfalse"),
                "Should contain 'Registry Client false', found: " + result.stdout);
        Assertions.assertFalse(Boolean.getBoolean("quarkusRegistryClient"),
                "Registry Client property should be set to false");

        // Dry run: Make sure registry client property is set (default = false) TODO
        result = CliDriver.execute(workspaceRoot, "ext", "list", "-e",
                "--dry-run");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);

        noSpaces = result.stdout.replaceAll("[\\s\\p{Z}]", "");
        Assertions.assertTrue(noSpaces.contains("RegistryClienttrue"),
                "Should contain 'Registry Client false', found: " + result.stdout);
        Assertions.assertFalse(Boolean.getBoolean("quarkusRegistryClient"),
                "Registry Client property should be set to false");
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
    public void testRegistryStreams() throws Exception {

        CliDriver.Result result;

        // refresh the local cache and list the registries
        result = CliDriver.execute(workspaceRoot, "registry", "--streams", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);
        try (BufferedReader reader = new BufferedReader(new StringReader(result.stdout))) {
            String line = reader.readLine();
            while (line != null && !TEST_QUARKUS_REGISTRY.equals(line)) {
                line = reader.readLine();
            }
            if (line == null) {
                Assertions.fail("Failed to locate registry " + TEST_QUARKUS_REGISTRY + " in the output");
            }
            line = reader.readLine();
            Assertions.assertNotNull(line, "Expected stream");
            final String expectedStream = getRequiredProperty("project.groupId") + ":" + getRequiredProperty("project.version");
            Assertions.assertTrue(line.contains(expectedStream), expectedStream);

            line = reader.readLine();
            Assertions.assertNotNull(line);
            Assertions.assertTrue(line.startsWith("(Read from "), "Expected (Read from ...");
            Assertions.assertNull(reader.readLine(), "No further content expected");
        }
    }

    @Test
    public void testRegistryRefresh() throws Exception {

        CliDriver.Result result;

        // refresh the local cache and list the registries
        result = CliDriver.execute(workspaceRoot, "registry", "--refresh", "-e");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);

        Path configPath = resolveConfigPath("enabledConfig.yml");
        result = CliDriver.execute(workspaceRoot, "registry", "--refresh", "-e",
                "--tools-config", configPath.toAbsolutePath().toString());
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains(configPath.toString()),
                "Should contain path to config file, found: " + result.stdout);
        Assertions.assertTrue(result.stdout.contains("registry.test.local"),
                "Should contain 'registry.test.local', found: " + result.stdout);
        Assertions.assertFalse(result.stdout.contains("registry.quarkus.io"),
                "Should not contain 'registry.quarkus.io', found: " + result.stdout);

        configPath = resolveConfigPath("disabledConfig.yml");
        result = CliDriver.execute(workspaceRoot, "registry", "--refresh", "-e",
                "--tools-config", configPath.toAbsolutePath().toString());
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains(configPath.toString()),
                "Should contain path to config file, found: " + result.stdout);
        Assertions.assertTrue(result.stdout.contains("registry.test.local (disabled)"),
                "Should contain '- registry.test.local (disabled)', found: " + result.stdout);
        Assertions.assertTrue(result.stdout.contains("registry.quarkus.io"),
                "Should contain '- registry.quarkus.io', found: " + result.stdout);
    }

    @Test
    public void testRegistryAddRemove() throws Exception {

        CliDriver.Result result;

        final Path testConfigYaml = workspaceRoot.resolve("test-registry-add-remove.yaml").toAbsolutePath();
        Files.deleteIfExists(testConfigYaml);

        assertThat(testConfigYaml).doesNotExist();
        result = CliDriver.execute(workspaceRoot, "registry", "add", "one,two", "--config", testConfigYaml.toString());
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);

        assertThat(testConfigYaml).exists();
        RegistriesConfig testConfig = RegistriesConfigLocator.load(testConfigYaml);
        assertThat(testConfig.getRegistries()).hasSize(2);
        assertThat(testConfig.getRegistries().get(0).getId()).isEqualTo("one");
        assertThat(testConfig.getRegistries().get(1).getId()).isEqualTo("two");

        result = CliDriver.execute(workspaceRoot, "registry", "add", "two,three", "--config", testConfigYaml.toString());
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);

        testConfig = RegistriesConfigLocator.load(testConfigYaml);
        assertThat(testConfig.getRegistries()).hasSize(3);
        assertThat(testConfig.getRegistries().get(0).getId()).isEqualTo("one");
        assertThat(testConfig.getRegistries().get(1).getId()).isEqualTo("two");
        assertThat(testConfig.getRegistries().get(2).getId()).isEqualTo("three");

        result = CliDriver.execute(workspaceRoot, "registry", "remove", "one,two", "--config", testConfigYaml.toString());
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);

        testConfig = RegistriesConfigLocator.load(testConfigYaml);
        assertThat(testConfig.getRegistries()).hasSize(1);
        assertThat(testConfig.getRegistries().get(0).getId()).isEqualTo("three");

        result = CliDriver.execute(workspaceRoot, "registry", "add", "four", "--config", testConfigYaml.toString());
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);

        testConfig = RegistriesConfigLocator.load(testConfigYaml);
        assertThat(testConfig.getRegistries()).hasSize(2);
        assertThat(testConfig.getRegistries().get(0).getId()).isEqualTo("three");
        assertThat(testConfig.getRegistries().get(1).getId()).isEqualTo("four");

        result = CliDriver.execute(workspaceRoot, "registry", "remove", "three,four,five", "--config",
                testConfigYaml.toString());
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code." + result);

        testConfig = RegistriesConfigMapperHelper.deserialize(testConfigYaml, JsonRegistriesConfig.class);
        assertThat(testConfig.getRegistries()).isEmpty();

        testConfig = RegistriesConfigLocator.load(testConfigYaml);
        assertThat(testConfig.getRegistries()).hasSize(1);
        assertThat(testConfig.getRegistries().get(0).getId()).isEqualTo("registry.quarkus.io");

        Files.delete(testConfigYaml);
    }

    private static String getRequiredProperty(String name) {
        return Objects.requireNonNull(System.getProperty(name));

    }

    private static Path resolveConfigPath(String configName) throws URISyntaxException {
        final URL configUrl = Thread.currentThread().getContextClassLoader().getResource(configName);
        assertThat(configUrl).isNotNull();
        final Path path = Paths.get(configUrl.toURI());
        return path;
    }
}
