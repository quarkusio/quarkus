package io.quarkus.cli.deploy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.cli.CliDriver;
import io.quarkus.cli.build.ExecuteUtil;
import io.quarkus.cli.build.GradleRunner;
import io.quarkus.devtools.testing.RegistryClientTestHelper;
import picocli.CommandLine;

/**
 * Similar to CliProjecGradleTest ..
 */
public class CliDeployGradleTest {

    static final Path testProjectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("target/test-classes/test-project/");
    static final Path workspaceRoot = testProjectRoot.resolve("CliImageGradleTest");
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
        CliDriver.deleteDir(wrapperRoot);

        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--gradle", "--verbose", "-e", "-B",
                "--no-code",
                "-o", testProjectRoot.toString(),
                "gradle-wrapper");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        Assertions.assertTrue(result.getStdout().contains("SUCCESS"),
                "Expected confirmation that the project has been created." + result);

        gradle = ExecuteUtil.findWrapper(wrapperRoot, GradleRunner.windowsWrapper, GradleRunner.otherWrapper);

        List<String> args = new ArrayList<>();
        args.add(gradle.getAbsolutePath());
        args.add("--daemon");
        CliDriver.preserveLocalRepoSettings(args);

        result = CliDriver.executeArbitraryCommand(wrapperRoot, args.toArray(new String[0]));
        Assertions.assertEquals(0, result.getExitCode(), "Gradle daemon should start properly");
    }

    @AfterAll
    static void stopGradleDaemon() throws Exception {
        if (gradle != null) {
            List<String> args = new ArrayList<>();
            args.add(gradle.getAbsolutePath());
            args.add("--stop");
            CliDriver.preserveLocalRepoSettings(args);

            CliDriver.Result result = CliDriver.executeArbitraryCommand(wrapperRoot, args.toArray(new String[0]));
            Assertions.assertEquals(0, result.getExitCode(), "Gradle daemon should stop properly");
        }
    }

    @Test
    public void testUsage() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--gradle", "--verbose", "-e", "-B");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);

        result = CliDriver.execute(project, "deploy", "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        assertTrue(result.getStdout().contains("deploy"));
        assertTrue(result.getStdout().contains("--no-daemon"));
        assertTrue(result.getStdout().contains("--no-build-cache"));

        testDeployer("kubernetes", "kubernetes", "docker");
        testDeployer("minikube", "kubernetes", "docker");
        testDeployer("kind", "kubernetes", "docker");
        testDeployer("openshift", "openshift", "openshift");
        testDeployer("knative", "knative", "docker");

    }

    protected void testDeployer(String deployer, String configGroup, String defaultImageBuilder) throws Exception {
        CliDriver.Result result = CliDriver.execute(project, "deploy", deployer, "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        assertTrue(result.getStdout().contains("deploy"));
        assertTrue(result.getStdout().contains("--no-daemon"));
        assertTrue(result.getStdout().contains("--no-build-cache"));
        assertTrue(result.getStdout().contains("-Dquarkus." + deployer + ".deploy=true"));
        assertTrue(result.getStdout().contains("--init-script="));
        assertFalse(result.getStdout().contains("-Dquarkus." + configGroup + ".deployment-kind"));

        result = CliDriver.execute(project, "deploy", deployer, "--image-build", "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        assertTrue(result.getStdout().contains("deploy"));
        assertTrue(result.getStdout().contains("--no-daemon"));
        assertTrue(result.getStdout().contains("--no-build-cache"));
        assertTrue(result.getStdout().contains("-Dquarkus." + deployer + ".deploy=true"));
        assertTrue(result.getStdout().contains("--init-script="));
        assertTrue(result.getStdout().contains("--image-builder=" + defaultImageBuilder));
        assertFalse(result.getStdout().contains("-Dquarkus." + configGroup + ".deployment-kind"));

        result = CliDriver.execute(project, "deploy", deployer, "--image-builder=jib", "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        assertTrue(result.getStdout().contains("deploy"));
        assertTrue(result.getStdout().contains("--no-daemon"));
        assertTrue(result.getStdout().contains("--no-build-cache"));
        assertTrue(result.getStdout().contains("-Dquarkus." + deployer + ".deploy=true"));
        assertTrue(result.getStdout().contains("--init-script="));
        assertTrue(result.getStdout().contains("--image-builder=jib"));
        assertFalse(result.getStdout().contains("-Dquarkus." + configGroup + ".deployment-kind"));

        if ("knative".equals(deployer)) {
            return;
        }
        result = CliDriver.execute(project, "deploy", deployer, "--deployment-kind", "Deployment",
                "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        assertTrue(result.getStdout().contains("deploy"));
        assertTrue(result.getStdout().contains("--no-daemon"));
        assertTrue(result.getStdout().contains("--no-build-cache"));
        assertTrue(result.getStdout().contains("-Dquarkus." + deployer + ".deploy=true"));
        assertTrue(result.getStdout().contains("--init-script="));
        assertTrue(result.getStdout().contains("-Dquarkus." + configGroup + ".deployment-kind=Deployment"));
    }
}
