package io.quarkus.cli;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.cli.build.ExecuteUtil;
import io.quarkus.cli.build.GradleRunner;
import io.quarkus.devtools.project.codegen.CreateProjectHelper;
import io.quarkus.devtools.testing.RegistryClientTestHelper;
import picocli.CommandLine;

/**
 * Similar to CliProjectMavenTest ..
 */
@Tag("failsOnJDK16")
public class CliProjectGradleTest {
    static final Path testProjectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("target/test-project/");
    static final Path workspaceRoot = testProjectRoot.resolve("CliProjectGradleTest");
    static final Path wrapperRoot = testProjectRoot.resolve("gradle-wrapper");

    Path project;
    static File gradle;

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

    @BeforeAll
    static void startGradleDaemon() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--gradle", "--verbose", "-e", "-B",
                "--no-code",
                "-o", testProjectRoot.toString(),
                "gradle-wrapper");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("SUCCESS"),
                "Expected confirmation that the project has been created." + result);

        gradle = ExecuteUtil.findWrapper(wrapperRoot, GradleRunner.windowsWrapper, GradleRunner.otherWrapper);

        List<String> args = new ArrayList<>();
        args.add(gradle.getAbsolutePath());
        args.add("--daemon");
        CliDriver.preserveLocalRepoSettings(args);

        result = CliDriver.executeArbitraryCommand(wrapperRoot, args.toArray(new String[0]));
        Assertions.assertEquals(0, result.exitCode, "Gradle daemon should start properly");
    }

    @AfterAll
    static void stopGradleDaemon() throws Exception {
        if (gradle != null) {
            List<String> args = new ArrayList<>();
            args.add(gradle.getAbsolutePath());
            args.add("--stop");
            CliDriver.preserveLocalRepoSettings(args);

            CliDriver.Result result = CliDriver.executeArbitraryCommand(wrapperRoot, args.toArray(new String[0]));
            Assertions.assertEquals(0, result.exitCode, "Gradle daemon should stop properly");
        }
    }

    @Test
    public void testNoCode() throws Exception {
        // Inspect the no-code project created to hold the gradle wrapper
        Assertions.assertTrue(gradle.exists(), "Wrapper should exist");

        Path packagePath = wrapperRoot.resolve("src/main/java/");
        Assertions.assertTrue(packagePath.toFile().isDirectory(),
                "Source directory should exist: " + packagePath.toAbsolutePath());

        String[] files = packagePath.toFile().list();
        Assertions.assertEquals(0, files.length,
                "Source directory should be empty: " + Arrays.toString(files));
    }

    @Test
    public void testCreateAppDefaults() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--gradle", "--verbose", "-e", "-B");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("SUCCESS"),
                "Expected confirmation that the project has been created." + result);

        Assertions.assertTrue(project.resolve("gradlew").toFile().exists(),
                "Wrapper should exist by default");
        String buildGradleContent = validateBasicIdentifiers(project, CreateProjectHelper.DEFAULT_GROUP_ID,
                CreateProjectHelper.DEFAULT_ARTIFACT_ID,
                CreateProjectHelper.DEFAULT_VERSION);
        Assertions.assertTrue(buildGradleContent.contains("quarkus-resteasy"),
                "build/gradle should contain quarkus-resteasy:\n" + buildGradleContent);

        CliDriver.valdiateGeneratedSourcePackage(project, "org/acme");

        CliDriver.invokeValidateBuild(project);
    }

    @Test
    public void testCreateAppOverrides() throws Exception {
        Path nested = workspaceRoot.resolve("cli-nested");
        project = nested.resolve("my-project");

        List<String> configs = Arrays.asList("custom.app.config1=val1",
                "custom.app.config2=val2", "lib.config=val3");

        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--gradle", "--verbose", "-e", "-B",
                "--package-name=custom.pkg",
                "--output-directory=" + nested,
                "--app-config=" + String.join(",", configs),
                "-x resteasy-reactive",
                "silly:my-project:0.1.0");

        // TODO: would love a test that doesn't use a wrapper, but CI path..

        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("SUCCESS"),
                "Expected confirmation that the project has been created." + result);

        Assertions.assertTrue(project.resolve("gradlew").toFile().exists(),
                "Wrapper should exist by default");
        String buildGradleContent = validateBasicIdentifiers(project, "silly", "my-project", "0.1.0");
        Assertions.assertTrue(buildGradleContent.contains("quarkus-resteasy-reactive"),
                "build.gradle should contain quarkus-resteasy-reactive:\n" + buildGradleContent);

        CliDriver.valdiateGeneratedSourcePackage(project, "custom/pkg");
        CliDriver.validateApplicationProperties(project, configs);

        result = CliDriver.invokeValidateDryRunBuild(project);
        Assertions.assertTrue(result.stdout.contains("-Dproperty=value1 -Dproperty2=value2"),
                "result should contain '-Dproperty=value1 -Dproperty2=value2':\n" + result.stdout);

        CliDriver.invokeValidateBuild(project);
    }

    @Test
    public void testCreateCliDefaults() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "cli", "--gradle", "--verbose", "-e", "-B");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("SUCCESS"),
                "Expected confirmation that the project has been created." + result);

        Assertions.assertTrue(project.resolve("gradlew").toFile().exists(),
                "Wrapper should exist by default");
        String buildGradleContent = validateBasicIdentifiers(project, CreateProjectHelper.DEFAULT_GROUP_ID,
                CreateProjectHelper.DEFAULT_ARTIFACT_ID,
                CreateProjectHelper.DEFAULT_VERSION);
        Assertions.assertFalse(buildGradleContent.contains("quarkus-resteasy"),
                "build/gradle should not contain quarkus-resteasy:\n" + buildGradleContent);
        Assertions.assertTrue(buildGradleContent.contains("quarkus-picocli"),
                "build/gradle should contain quarkus-picocli:\n" + buildGradleContent);

        CliDriver.valdiateGeneratedSourcePackage(project, "org/acme");

        CliDriver.invokeValidateBuild(project);
    }

    @Test
    public void testExtensionList() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--gradle", "--verbose", "-e", "-B");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);

        Path buildGradle = project.resolve("build.gradle");
        String buildGradleContent = CliDriver.readFileAsString(project, buildGradle);
        Assertions.assertFalse(buildGradleContent.contains("quarkus-qute"),
                "Dependencies should not contain qute extension by default. Found:\n" + buildGradleContent);

        CliDriver.invokeExtensionAddQute(project, buildGradle);
        CliDriver.invokeExtensionAddRedundantQute(project);
        CliDriver.invokeExtensionListInstallable(project);
        CliDriver.invokeExtensionAddMultiple(project, buildGradle);
        CliDriver.invokeExtensionRemoveQute(project, buildGradle);
        CliDriver.invokeExtensionRemoveMultiple(project, buildGradle);

        CliDriver.invokeExtensionListInstallableSearch(project);
        CliDriver.invokeExtensionListFormatting(project);

        // TODO: Maven and Gradle give different return codes
        result = CliDriver.invokeExtensionRemoveNonexistent(project);
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
    }

    @Test
    public void testBuildOptions() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--gradle", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);

        // 1 --clean --tests --native --offline
        result = CliDriver.execute(project, "build", "-e", "-B", "--dry-run",
                "--clean", "--tests", "--native", "--offline");

        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        Assertions.assertTrue(result.stdout.contains(" clean"),
                "gradle command should specify 'clean'\n" + result);

        Assertions.assertFalse(result.stdout.contains("-x test"),
                "gradle command should not specify '-x test'\n" + result);

        Assertions.assertTrue(result.stdout.contains("-Dquarkus.package.type=native"),
                "gradle command should specify -Dquarkus.package.type=native\n" + result);

        Assertions.assertTrue(result.stdout.contains("--offline"),
                "gradle command should specify --offline\n" + result);

        // 2 --no-clean --no-tests
        result = CliDriver.execute(project, "build", "-e", "-B", "--dry-run",
                "--no-clean", "--no-tests");

        Assertions.assertFalse(result.stdout.contains(" clean"),
                "gradle command should not specify 'clean'\n" + result);

        Assertions.assertTrue(result.stdout.contains("-x test"),
                "gradle command should specify '-x test'\n" + result);

        Assertions.assertFalse(result.stdout.contains("native"),
                "gradle command should not specify native\n" + result);

        Assertions.assertFalse(result.stdout.contains("offline"),
                "gradle command should not specify offline\n" + result);
    }

    @Test
    public void testDevOptions() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--gradle", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);

        // 1 --clean --tests --suspend
        result = CliDriver.execute(project, "dev", "-e", "--dry-run",
                "--clean", "--tests", "--debug", "--suspend", "--debug-mode=listen");

        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("GRADLE"),
                "gradle command should specify 'GRADLE'\n" + result);

        Assertions.assertTrue(result.stdout.contains(" clean"),
                "gradle command should specify 'clean'\n" + result);

        Assertions.assertFalse(result.stdout.contains("-x test"),
                "gradle command should not specify '-x test'\n" + result);

        Assertions.assertFalse(result.stdout.contains("-Ddebug"),
                "gradle command should not specify '-Ddebug'\n" + result);

        Assertions.assertTrue(result.stdout.contains("-Dsuspend"),
                "gradle command should specify '-Dsuspend'\n" + result);

        // 2 --no-clean --no-tests --no-debug
        result = CliDriver.execute(project, "dev", "-e", "--dry-run",
                "--no-clean", "--no-tests", "--no-debug");

        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("GRADLE"),
                "gradle command should specify 'GRADLE'\n" + result);

        Assertions.assertFalse(result.stdout.contains(" clean"),
                "gradle command should not specify 'clean'\n" + result);

        Assertions.assertTrue(result.stdout.contains("-x test"),
                "gradle command should specify '-x test'\n" + result);

        Assertions.assertTrue(result.stdout.contains("-Ddebug=false"),
                "gradle command should specify '-Ddebug=false'\n" + result);

        Assertions.assertFalse(result.stdout.contains("-Dsuspend"),
                "gradle command should not specify '-Dsuspend'\n" + result);

        // 3 --no-suspend --debug-host=0.0.0.0 --debug-port=8008 --debug-mode=connect -- arg1 arg2
        result = CliDriver.execute(project, "dev", "-e", "--dry-run",
                "--no-suspend", "--debug-host=0.0.0.0", "--debug-port=8008", "--debug-mode=connect", "--", "arg1", "arg2");

        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("GRADLE"),
                "gradle command should specify 'GRADLE'\n" + result);

        Assertions.assertTrue(
                result.stdout.contains("-DdebugHost=0.0.0.0 -Ddebug=client -DdebugPort=8008"),
                "gradle command should specify -DdebugHost=0.0.0.0 -Ddebug=client -DdebugPort=8008\n" + result);

        Assertions.assertFalse(result.stdout.contains("-Dsuspend"),
                "gradle command should not specify '-Dsuspend'\n" + result);

        Assertions.assertTrue(result.stdout.contains("-Dquarkus.args='arg1 arg2'"),
                "gradle command should not specify -Dquarkus.args='arg1 arg2'\n" + result);
    }

    @Test
    public void testCreateArgPassthrough() throws Exception {
        Path nested = workspaceRoot.resolve("cli-nested");
        project = nested.resolve("my-project");

        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "--gradle",
                "--verbose", "-e", "-B",
                "--dryrun", "--no-wrapper", "--package-name=custom.pkg",
                "--output-directory=" + nested,
                "silly:my-project:0.1.0");

        // We don't need to retest this, just need to make sure all of the arguments were passed through
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);

        Assertions.assertTrue(result.stdout.contains("Creating an app"),
                "Should contain 'Creating an app', found: " + result.stdout);
        Assertions.assertTrue(result.stdout.contains("GRADLE"),
                "Should contain MAVEN, found: " + result.stdout);

        // strip spaces to avoid fighting with column whitespace
        String noSpaces = result.stdout.replaceAll("[\\s\\p{Z}]", "");
        Assertions.assertTrue(noSpaces.contains("Omitbuildtoolwrappertrue"),
                "Should contain 'Omit build tool wrapper   true', found: " + result.stdout);
        Assertions.assertTrue(noSpaces.contains("PackageNamecustom.pkg"),
                "Should contain 'Package Name   custom.pkg', found: " + result.stdout);
        Assertions.assertTrue(noSpaces.contains("ProjectArtifactIdmy-project"),
                "Output should contain 'Project ArtifactId   my-project', found: " + result.stdout);
        Assertions.assertTrue(noSpaces.contains("ProjectGroupIdsilly"),
                "Output should contain 'Project GroupId   silly', found: " + result.stdout);
        Assertions.assertTrue(result.stdout.contains("JAVA"),
                "Should contain JAVA, found: " + result.stdout);
    }

    String validateBasicIdentifiers(Path project, String group, String artifact, String version) throws Exception {
        Path buildGradle = project.resolve("build.gradle");
        Assertions.assertTrue(buildGradle.toFile().exists(),
                "build.gradle should exist: " + buildGradle.toAbsolutePath().toString());

        String buildContent = CliDriver.readFileAsString(project, buildGradle);
        Assertions.assertTrue(buildContent.contains("group '" + group + "'"),
                "build.gradle should include the group id:\n" + buildContent);
        Assertions.assertTrue(buildContent.contains("version '" + version + "'"),
                "build.gradle should include the version:\n" + buildContent);

        Path settings = project.resolve("settings.gradle");
        Assertions.assertTrue(settings.toFile().exists(),
                "settings.gradle should exist: " + settings.toAbsolutePath().toString());
        String settingsContent = CliDriver.readFileAsString(project, settings);
        Assertions.assertTrue(settingsContent.contains(artifact),
                "settings.gradle should include the artifact id:\n" + settingsContent);

        return buildContent;
    }
}
