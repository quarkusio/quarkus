package io.quarkus.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.project.codegen.CreateProjectHelper;
import picocli.CommandLine;

/**
 * Similar to CliProjectGradleTest ..
 */
public class CliProjectMavenTest {
    static String startingDir;
    static Path workspaceRoot;
    Path project;

    @BeforeAll
    public static void initial() throws Exception {
        startingDir = System.getProperty("user.dir");
        workspaceRoot = Paths.get(startingDir).toAbsolutePath().resolve("target/test-project/CliProjectMavenTest");
    }

    @BeforeEach
    public void setupTestDirectories() throws Exception {
        System.setProperty("user.dir", workspaceRoot.toFile().getAbsolutePath());
        CliDriver.deleteDir(workspaceRoot);
        project = workspaceRoot.resolve("code-with-quarkus");
    }

    @AfterAll
    public static void allDone() {
        System.setProperty("user.dir", startingDir);
    }

    @Test
    public void testCreateAppDefaults() throws Exception {
        CliDriver.Result result = CliDriver.execute("create", "app", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("SUCCESS"),
                "Expected confirmation that the project has been created." + result);

        Assertions.assertTrue(project.resolve("mvnw").toFile().exists(),
                "Wrapper should exist by default");
        String pomContent = validateBasicIdentifiers(CreateProjectHelper.DEFAULT_GROUP_ID,
                CreateProjectHelper.DEFAULT_ARTIFACT_ID,
                CreateProjectHelper.DEFAULT_VERSION);
        Assertions.assertTrue(pomContent.contains("<artifactId>quarkus-resteasy</artifactId>"),
                "pom.xml should contain quarkus-resteasy:\n" + pomContent);

        CliDriver.valdiateGeneratedSourcePackage(project, "org/acme");

        System.setProperty("user.dir", project.toFile().getAbsolutePath());
        CliDriver.invokeValidateBuild(project);
    }

    @Test
    public void testCreateAppGAVNoWrapper() throws Exception {
        project = workspaceRoot.resolve("nested/my-project");

        List<String> configs = Arrays.asList("custom.app.config1=val1",
                "custom.app.config2=val2", "lib.config=val3");

        CliDriver.Result result = CliDriver.execute("create", "app", "--verbose", "-e", "-B",
                "--no-wrapper", "--package-name=custom.pkg",
                "--output-directory=nested",
                "--group-id=silly", "--artifact-id=my-project", "--version=0.1.0",
                "--app-config=" + String.join(",", configs),
                "resteasy-reactive");

        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("SUCCESS"),
                "Expected confirmation that the project has been created." + result);

        Assertions.assertFalse(project.resolve("mvnw").toFile().exists(),
                "Wrapper should not exist when specifying --no-wrapper");
        String pomContent = validateBasicIdentifiers("silly", "my-project", "0.1.0");
        Assertions.assertTrue(pomContent.contains("<artifactId>quarkus-resteasy-reactive</artifactId>"),
                "pom.xml should contain quarkus-resteasy-reactive:\n" + pomContent);

        CliDriver.valdiateGeneratedSourcePackage(project, "custom/pkg");
        CliDriver.validateApplicationProperties(project, configs);

        System.setProperty("user.dir", project.toFile().getAbsolutePath());
        CliDriver.invokeValidateBuild(project);
    }

    @Test
    public void testExtensionList() throws Exception {
        CliDriver.Result result = CliDriver.execute("create", "app", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);

        System.setProperty("user.dir", project.toFile().getAbsolutePath());

        Path pom = project.resolve("pom.xml");
        String pomContent = CliDriver.readFileAsString(pom);
        Assertions.assertFalse(pomContent.contains("quarkus-qute"),
                "Dependencies should not contain qute extension by default. Found:\n" + pomContent);

        CliDriver.invokeExtensionAddQute(pom);
        CliDriver.invokeExtensionAddRedundantQute();
        CliDriver.invokeExtensionListInstallable();
        CliDriver.invokeExtensionAddMultiple(pom);
        CliDriver.invokeExtensionRemoveQute(pom);
        CliDriver.invokeExtensionRemoveMultiple(pom);

        CliDriver.invokeExtensionListInstallableSearch();
        CliDriver.invokeExtensionListFormatting();

        // TODO: Maven and Gradle give different return codes
        result = CliDriver.invokeExtensionRemoveNonexistent();
        Assertions.assertEquals(CommandLine.ExitCode.SOFTWARE, result.exitCode,
                "Expected error return code. Result:\n" + result);
    }

    @Test
    public void testCreateArgPassthrough() throws Exception {
        CliDriver.Result result = CliDriver.execute("create",
                "--verbose", "-e", "-B",
                "--dryrun", "--no-wrapper", "--package-name=custom.pkg",
                "--output-directory=nested",
                "--group-id=silly", "--artifact-id=my-project", "--version=0.1.0");

        // We don't need to retest this, just need to make sure all of the arguments were passed through
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);

        Assertions.assertTrue(result.stdout.contains("No subcommand specified, creating an app (see --help)"),
                "Should contain 'No subcommand specified, creating an app (see --help)', found: " + result.stdout);
        Assertions.assertTrue(result.stdout.contains("MAVEN"),
                "Should contain MAVEN, found: " + result.stdout);
        Assertions.assertTrue(result.stdout.contains("Omit build tool wrapper   true"),
                "Should contain 'Omit build tool wrapper   true', found: " + result.stdout);
        Assertions.assertTrue(result.stdout.contains("Package Name              custom.pkg"),
                "Should contain 'Package Name              custom.pkg', found: " + result.stdout);
        Assertions.assertTrue(result.stdout.contains("Project ArtifactId        my-project"),
                "Output should contain 'Project ArtifactId        my-project', found: " + result.stdout);
        Assertions.assertTrue(result.stdout.contains("Project GroupId           silly"),
                "Output should contain 'Project GroupId           silly', found: " + result.stdout);
        Assertions.assertTrue(result.stdout.contains("JAVA"),
                "Should contain JAVA, found: " + result.stdout);
    }

    String validateBasicIdentifiers(String group, String artifact, String version) throws Exception {
        Path pom = project.resolve("pom.xml");

        Assertions.assertTrue(pom.toFile().exists(),
                "pom.xml should exist: " + pom.toAbsolutePath().toString());
        String pomContent = CliDriver.readFileAsString(pom);
        Assertions.assertTrue(pomContent.contains("<groupId>" + group + "</groupId>"),
                "pom.xml should contain group id:\n" + pomContent);
        Assertions.assertTrue(pomContent.contains("<artifactId>" + artifact + "</artifactId>"),
                "pom.xml should contain artifact id:\n" + pomContent);
        Assertions.assertTrue(pomContent.contains("<version>" + version + "</version>"),
                "pom.xml should contain version:\n" + pomContent);
        return pomContent;
    }
}
