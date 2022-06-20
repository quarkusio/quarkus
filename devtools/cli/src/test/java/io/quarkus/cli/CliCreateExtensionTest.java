package io.quarkus.cli;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.commands.CreateExtension;
import io.quarkus.devtools.testing.RegistryClientTestHelper;
import picocli.CommandLine;

public class CliCreateExtensionTest {
    static final Path testProjectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("target/test-project/");
    static final Path workspaceRoot = testProjectRoot.resolve("CliCreateExtensionTest");
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
        project = workspaceRoot.resolve("quarkus-custom");
    }

    @Test
    public void testCreateDryRun() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "extension", "--dry-run");
        String noSpaces = result.stdout.replaceAll("[\\s\\p{Z}]", "");
        Assertions.assertTrue(noSpaces.contains("SkipDev-modeTestfalse"),
                "Skip Dev-mode Test should be false. Found:\n" + result);
        Assertions.assertTrue(noSpaces.contains("SkipIntegrationTestfalse"),
                "Skip Integration Test should be false. Found:\n" + result);
        Assertions.assertTrue(noSpaces.contains("SkipUnitTestfalse"),
                "Skip Unit test should be false. Found:\n" + result);
    }

    @Test
    public void testCreateDryRunWithoutTests() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "extension", "--dry-run", "--without-tests");
        String noSpaces = result.stdout.replaceAll("[\\s\\p{Z}]", "");
        Assertions.assertTrue(noSpaces.contains("SkipDev-modeTesttrue"),
                "Skip Dev-mode Test should be true. Found:\n" + result);
        Assertions.assertTrue(noSpaces.contains("SkipIntegrationTesttrue"),
                "Skip Integration Test should be true. Found:\n" + result);
        Assertions.assertTrue(noSpaces.contains("SkipUnitTesttrue"),
                "Skip Unit test should be true. Found:\n" + result);
    }

    @Test
    public void testCreateDryRunNoTests() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "extension", "--dry-run",
                "--no-unit-test", "--no-it-test", "--no-devmode-test");
        String noSpaces = result.stdout.replaceAll("[\\s\\p{Z}]", "");
        Assertions.assertTrue(noSpaces.contains("SkipDev-modeTesttrue"),
                "Skip Dev-mode Test should be true. Found:\n" + result);
        Assertions.assertTrue(noSpaces.contains("SkipIntegrationTesttrue"),
                "Skip Integration Test should be true. Found:\n" + result);
        Assertions.assertTrue(noSpaces.contains("SkipUnitTesttrue"),
                "Skip Unit test should be true. Found:\n" + result);
    }

    @Test
    public void testCreateExtensionDefaults() throws Exception {
        // Create a Quarkiverse extension by default
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "extension", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("generated"),
                "Expected confirmation that the project has been created." + result);

        Path projectPom = project.resolve("pom.xml");
        validateBasicIdentifiers(projectPom, // <parent> pom
                CreateExtension.DEFAULT_QUARKIVERSE_PARENT_GROUP_ID,
                CreateExtension.DEFAULT_QUARKIVERSE_PARENT_ARTIFACT_ID,
                CreateExtension.DEFAULT_QUARKIVERSE_PARENT_VERSION);
        validateBasicIdentifiers(projectPom,
                "io.quarkiverse.custom",
                "quarkus-custom-parent",
                CreateExtension.DEFAULT_VERSION);

        Path runtimePom = project.resolve("runtime/pom.xml");
        validateBasicIdentifiers(runtimePom, // <parent> pom
                "io.quarkiverse.custom",
                "quarkus-custom-parent",
                CreateExtension.DEFAULT_VERSION);
        validateBasicIdentifiers(runtimePom,
                "io.quarkiverse.custom",
                "quarkus-custom",
                CreateExtension.DEFAULT_VERSION);
        Path quarkusExtension = project.resolve("runtime/src/main/resources/META-INF/quarkus-extension.yaml");
        Assertions.assertTrue(quarkusExtension.toFile().exists(),
                "quarkus-extension.yaml should exist: " + quarkusExtension.toAbsolutePath().toString());

        Path deploymentPom = project.resolve("deployment/pom.xml");
        validateBasicIdentifiers(deploymentPom, // <parent> pom
                "io.quarkiverse.custom",
                "quarkus-custom-parent",
                CreateExtension.DEFAULT_VERSION);
        validateBasicIdentifiers(deploymentPom,
                "io.quarkiverse.custom",
                "quarkus-custom-deployment",
                CreateExtension.DEFAULT_VERSION);
        CliDriver.valdiateGeneratedSourcePackage(project.resolve("deployment"), "io/quarkiverse/custom/deployment");
        CliDriver.valdiateGeneratedTestPackage(project.resolve("deployment"), "io/quarkiverse/custom/test");

        Path itPom = project.resolve("integration-tests/pom.xml");
        validateBasicIdentifiers(itPom, // <parent> pom
                "io.quarkiverse.custom",
                "quarkus-custom-parent",
                CreateExtension.DEFAULT_VERSION);
        validateBasicIdentifiers(itPom,
                "io.quarkiverse.custom",
                "quarkus-custom-integration-tests",
                CreateExtension.DEFAULT_VERSION);
        CliDriver.valdiateGeneratedSourcePackage(project.resolve("integration-tests"), "io/quarkiverse/custom/it");
        CliDriver.valdiateGeneratedTestPackage(project.resolve("integration-tests"), "io/quarkiverse/custom/it");
    }

    @Test
    public void testCreateExtension() throws Exception {
        // Create a standalone extension w/ specified names
        project = workspaceRoot.resolve("something");
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "extension", "-e", "-B", "--verbose",
                "org.my:something:0.1");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("generated"),
                "Expected confirmation that the project has been created." + result);

        Path projectPom = project.resolve("pom.xml");
        validateBasicIdentifiers(projectPom,
                "org.my", "something-parent", "0.1");

        Path runtimePom = project.resolve("runtime/pom.xml");
        validateBasicIdentifiers(runtimePom, // <parent> pom
                "org.my", "something-parent", "0.1");
        validateBasicIdentifiers(runtimePom,
                "org.my", "something", "0.1");
        Path quarkusExtension = project.resolve("runtime/src/main/resources/META-INF/quarkus-extension.yaml");
        Assertions.assertTrue(quarkusExtension.toFile().exists(),
                "quarkus-extension.yaml should exist: " + quarkusExtension.toAbsolutePath().toString());

        Path deploymentPom = project.resolve("deployment/pom.xml");
        validateBasicIdentifiers(deploymentPom, // <parent> pom
                "org.my", "something-parent", "0.1");
        validateBasicIdentifiers(deploymentPom,
                "org.my", "something-deployment", "0.1");
        CliDriver.valdiateGeneratedSourcePackage(project.resolve("deployment"), "org/my/something/deployment");
        CliDriver.valdiateGeneratedTestPackage(project.resolve("deployment"), "org/my/something/test");

        Path itPom = project.resolve("integration-tests/pom.xml");
        validateBasicIdentifiers(itPom, // <parent> pom
                "org.my", "something-parent", "0.1");
        validateBasicIdentifiers(itPom,
                "org.my", "something-integration-tests", "0.1");
        CliDriver.valdiateGeneratedSourcePackage(project.resolve("integration-tests"), "org/my/something/it");
        CliDriver.valdiateGeneratedTestPackage(project.resolve("integration-tests"), "org/my/something/it");
    }

    String validateBasicIdentifiers(Path pom, String group, String artifact, String version) throws Exception {
        Assertions.assertTrue(pom.toFile().exists(),
                "pom.xml should exist: " + pom.toAbsolutePath().toString());

        String pomContent = CliDriver.readFileAsString(project, pom);
        Assertions.assertTrue(pomContent.contains("<groupId>" + group + "</groupId>"),
                pom + " should contain group id " + group + ":\n" + pomContent);
        Assertions.assertTrue(pomContent.contains("<artifactId>" + artifact + "</artifactId>"),
                pom + " should contain artifact id " + artifact + ":\n" + pomContent);
        Assertions.assertTrue(pomContent.contains("<version>" + version + "</version>"),
                pom + " should contain version " + version + ":\n" + pomContent);
        return pomContent;
    }
}
