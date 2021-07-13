package io.quarkus.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.project.codegen.CreateProjectHelper;
import picocli.CommandLine;

/**
 * Similar to CliProjectGradleTest ..
 */
public class CliProjectMavenTest {
    static Path workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("target/test-project/CliProjectMavenTest");
    Path project;

    @BeforeEach
    public void setupTestDirectories() throws Exception {
        CliDriver.deleteDir(workspaceRoot);
        project = workspaceRoot.resolve("code-with-quarkus");
    }

    @Test
    public void testCreateAppDefaults() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "-e", "-B", "--verbose");
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

        result = CliDriver.invokeValidateDryRunBuild(project);

        CliDriver.invokeValidateBuild(project);

        // Test create project that already exists
        result = CliDriver.execute(workspaceRoot, "create", "app", "-e", "-B", "--verbose");
        Assertions.assertTrue(result.stdout.contains("quarkus create app --help"),
                "Response should reference --help:\n" + result);
    }

    @Test
    public void testCreateAppOverrides() throws Exception {
        Path nested = workspaceRoot.resolve("cli-nested");
        project = nested.resolve("my-project");

        List<String> configs = Arrays.asList("custom.app.config1=val1",
                "custom.app.config2=val2", "lib.config=val3");

        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--verbose", "-e", "-B",
                "--no-wrapper", "--package-name=custom.pkg",
                "--output-directory=" + nested,
                "--app-config=" + String.join(",", configs),
                "-x resteasy-reactive,micrometer",
                "silly:my-project:0.1.0");

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

        result = CliDriver.invokeValidateDryRunBuild(project);
        Assertions.assertTrue(result.stdout.contains("-Dproperty=value1 -Dproperty2=value2"),
                "result should contain '-Dproperty=value1 -Dproperty2=value2':\n" + result.stdout);

        CliDriver.invokeValidateBuild(project);
    }

    @Test
    public void testExtensionList() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);

        Path pom = project.resolve("pom.xml");
        String pomContent = CliDriver.readFileAsString(project, pom);
        Assertions.assertFalse(pomContent.contains("quarkus-qute"),
                "Dependencies should not contain qute extension by default. Found:\n" + pomContent);

        CliDriver.invokeExtensionAddQute(project, pom);
        CliDriver.invokeExtensionAddRedundantQute(project);
        CliDriver.invokeExtensionListInstallable(project);
        CliDriver.invokeExtensionAddMultiple(project, pom);
        CliDriver.invokeExtensionRemoveQute(project, pom);
        CliDriver.invokeExtensionRemoveMultiple(project, pom);

        CliDriver.invokeExtensionListInstallableSearch(project);
        CliDriver.invokeExtensionListFormatting(project);

        // TODO: Maven and Gradle give different return codes
        result = CliDriver.invokeExtensionRemoveNonexistent(project);
        Assertions.assertEquals(CommandLine.ExitCode.SOFTWARE, result.exitCode,
                "Expected error return code. Result:\n" + result);
    }

    @Test
    public void testBuildOptions() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);

        // 1 --clean --tests --native --offline
        result = CliDriver.execute(project, "build", "-e", "-B", "--dry-run",
                "--clean", "--tests", "--native", "--offline");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        Assertions.assertTrue(result.stdout.contains(" clean"),
                "mvn command should specify 'clean'\n" + result);

        Assertions.assertFalse(result.stdout.contains("-DskipTests"),
                "mvn command should not specify -DskipTests\n" + result);
        Assertions.assertFalse(result.stdout.contains("-Dmaven.test.skip=true"),
                "mvn command should not specify -Dmaven.test.skip=true\n" + result);

        Assertions.assertTrue(result.stdout.contains("-Dnative"),
                "mvn command should specify -Dnative\n" + result);

        Assertions.assertTrue(result.stdout.contains("--offline"),
                "mvn command should specify --offline\n" + result);

        // 2 --no-clean --no-tests
        result = CliDriver.execute(project, "build", "-e", "-B", "--dry-run",
                "--no-clean", "--no-tests");

        Assertions.assertFalse(result.stdout.contains(" clean"),
                "mvn command should not specify 'clean'\n" + result);

        Assertions.assertTrue(result.stdout.contains("-DskipTests"),
                "mvn command should specify -DskipTests\n" + result);
        Assertions.assertTrue(result.stdout.contains("-Dmaven.test.skip=true"),
                "mvn command should specify -Dmaven.test.skip=true\n" + result);

        Assertions.assertFalse(result.stdout.contains("-Dnative"),
                "mvn command should not specify -Dnative\n" + result);

        Assertions.assertFalse(result.stdout.contains("--offline"),
                "mvn command should not specify --offline\n" + result);
    }

    @Test
    public void testDevOptions() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);

        // 1 --clean --tests --suspend
        result = CliDriver.execute(project, "dev", "-e", "--dry-run",
                "--clean", "--tests", "--debug", "--suspend", "--debug-mode=listen");

        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("MAVEN"),
                "quarkus command should specify 'MAVEN'\n" + result);

        Assertions.assertTrue(result.stdout.contains(" clean"),
                "mvn command should specify 'clean'\n" + result);

        Assertions.assertFalse(result.stdout.contains("-DskipTests"),
                "mvn command should not specify -DskipTests\n" + result);
        Assertions.assertFalse(result.stdout.contains("-Dmaven.test.skip=true"),
                "mvn command should not specify -Dmaven.test.skip=true\n" + result);

        Assertions.assertFalse(result.stdout.contains("-Ddebug"),
                "mvn command should not specify '-Ddebug'\n" + result);

        Assertions.assertTrue(result.stdout.contains("-Dsuspend"),
                "mvn command should specify '-Dsuspend'\n" + result);

        // 2 --no-clean --no-tests --no-debug
        result = CliDriver.execute(project, "dev", "-e", "--dry-run",
                "--no-clean", "--no-tests", "--no-debug");

        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("MAVEN"),
                "quarkus command should specify 'MAVEN'\n" + result);

        Assertions.assertFalse(result.stdout.contains(" clean"),
                "mvn command should not specify 'clean'\n" + result);

        Assertions.assertTrue(result.stdout.contains("-DskipTests"),
                "mvn command should specify -DskipTests\n" + result);
        Assertions.assertTrue(result.stdout.contains("-Dmaven.test.skip=true"),
                "mvn command should specify -Dmaven.test.skip=true\n" + result);

        Assertions.assertTrue(result.stdout.contains("-Ddebug=false"),
                "mvn command should specify '-Ddebug=false'\n" + result);

        Assertions.assertFalse(result.stdout.contains("-Dsuspend"),
                "mvn command should not specify '-Dsuspend'\n" + result);

        // 3 --no-suspend --debug-host=0.0.0.0 --debug-port=8008 --debug-mode=connect -- arg1 arg2
        result = CliDriver.execute(project, "dev", "-e", "--dry-run",
                "--no-suspend", "--debug-host=0.0.0.0", "--debug-port=8008", "--debug-mode=connect", "--", "arg1", "arg2");

        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("MAVEN"),
                "quarkus command should specify 'MAVEN'\n" + result);

        Assertions.assertTrue(
                result.stdout.contains("-DdebugHost=0.0.0.0 -Ddebug=client"),
                "mvn command should specify -DdebugHost=0.0.0.0 -Ddebug=client\n" + result);

        Assertions.assertFalse(result.stdout.contains("-Dsuspend"),
                "mvn command should not specify '-Dsuspend'\n" + result);

        Assertions.assertTrue(result.stdout.contains("-Dquarkus.args='arg1 arg2'"),
                "mvn command should not specify -Dquarkus.args='arg1 arg2'\n" + result);
    }

    @Test
    public void testCreateCliDefaults() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "cli", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("SUCCESS"),
                "Expected confirmation that the project has been created." + result);

        Assertions.assertTrue(project.resolve("mvnw").toFile().exists(),
                "Wrapper should exist by default");
        String pomContent = validateBasicIdentifiers(CreateProjectHelper.DEFAULT_GROUP_ID,
                CreateProjectHelper.DEFAULT_ARTIFACT_ID,
                CreateProjectHelper.DEFAULT_VERSION);
        Assertions.assertFalse(pomContent.contains("<artifactId>quarkus-resteasy</artifactId>"),
                "pom.xml should not contain quarkus-resteasy:\n" + pomContent);
        Assertions.assertTrue(pomContent.contains("<artifactId>quarkus-picocli</artifactId>"),
                "pom.xml should contain quarkus-picocli:\n" + pomContent);

        CliDriver.valdiateGeneratedSourcePackage(project, "org/acme");

        result = CliDriver.invokeValidateDryRunBuild(project);

        CliDriver.invokeValidateBuild(project);
    }

    @Test
    public void testCreateArgPassthrough() throws Exception {
        Path nested = workspaceRoot.resolve("cli-nested");
        project = nested.resolve("my-project");

        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create",
                "--verbose", "-e", "-B",
                "--dryrun", "--no-wrapper", "--package-name=custom.pkg",
                "-x resteasy-reactive",
                "-x micrometer",
                "--output-directory=" + nested,
                "silly:my-project:0.1.0");

        // We don't need to retest this, just need to make sure all of the arguments were passed through
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);

        Assertions.assertTrue(result.stdout.contains("Creating an app"),
                "Should contain 'Creating an app', found: " + result.stdout);
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
        String pomContent = CliDriver.readFileAsString(project, pom);
        Assertions.assertTrue(pomContent.contains("<groupId>" + group + "</groupId>"),
                "pom.xml should contain group id:\n" + pomContent);
        Assertions.assertTrue(pomContent.contains("<artifactId>" + artifact + "</artifactId>"),
                "pom.xml should contain artifact id:\n" + pomContent);
        Assertions.assertTrue(pomContent.contains("<version>" + version + "</version>"),
                "pom.xml should contain version:\n" + pomContent);
        return pomContent;
    }
}
