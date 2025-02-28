package io.quarkus.cli.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.cli.CliDriver;
import io.quarkus.devtools.testing.RegistryClientTestHelper;
import picocli.CommandLine;

/**
 * Similar to CliProjecMavenTest ..
 */
public class CliDeployMavenTest {
    static Path workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("target/test-classes/test-project/CliDeployMavenTest");
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
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);

        // 1 deploy --dry-run
        result = CliDriver.execute(project, "deploy", "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);

        testDeployer("kubernetes", "kubernetes", "docker");
        testDeployer("minikube", "kubernetes", "docker");
        testDeployer("kind", "kubernetes", "docker");
        testDeployer("openshift", "openshift", "openshift");
        testDeployer("knative", "knative", "docker");
    }

    protected void testDeployer(String deployer, String configGroup, String defaultImageBuilder) throws Exception {
        CliDriver.Result result = CliDriver.execute(project, "deploy", deployer, "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        assertTrue(result.getStdout().contains("quarkus:deploy"));
        assertTrue(result.getStdout().contains("-Dquarkus." + deployer + ".deploy=true"));
        assertTrue(result.getStdout().contains("-Dquarkus.container-image.build=false"));
        assertFalse(result.getStdout().contains("-Dquarkus." + configGroup + ".deployment-kind"));

        result = CliDriver.execute(project, "deploy", deployer, "--image-build", "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        assertTrue(result.getStdout().contains("quarkus:deploy"));
        assertTrue(result.getStdout().contains("-Dquarkus." + deployer + ".deploy=true"));
        assertTrue(result.getStdout().contains("-Dquarkus.container-image.builder=" + defaultImageBuilder));
        assertTrue(result.getStdout().contains("-Dquarkus.container-image.build=true"));
        assertFalse(result.getStdout().contains("-Dquarkus." + configGroup + ".deployment-kind"));

        result = CliDriver.execute(project, "deploy", deployer, "--image-builder=jib", "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        assertTrue(result.getStdout().contains("quarkus:deploy"));
        assertTrue(result.getStdout().contains("-Dquarkus." + deployer + ".deploy=true"));
        assertTrue(result.getStdout().contains("-Dquarkus.container-image.builder=jib"));
        assertTrue(result.getStdout().contains("-Dquarkus.container-image.build=true"));
        assertFalse(result.getStdout().contains("-Dquarkus." + configGroup + ".deployment-kind"));

        if ("knative".equals(deployer)) {
            return;
        }
        result = CliDriver.execute(project, "deploy", deployer, "--deployment-kind", "Deployment", "--dry-run");
        assertEquals(CommandLine.ExitCode.OK, result.getExitCode(), "Expected OK return code." + result);
        assertTrue(result.getStdout().contains("quarkus:deploy"));
        assertTrue(result.getStdout().contains("-Dquarkus." + deployer + ".deploy=true"));
        assertTrue(result.getStdout().contains("-Dquarkus.container-image.build=false"));
        assertTrue(result.getStdout().contains("-Dquarkus." + configGroup + ".deployment-kind=Deployment"));
    }
}
