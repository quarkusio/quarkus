package io.quarkus.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * This is ordered to make output easier to view (as it effectively dumps help)
 */
@TestMethodOrder(OrderAnnotation.class)
@QuarkusMainTest
public class CliHelpTest {
    static Path workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath().resolve("target/test-project");

    @Test
    @Order(1)
    public void testCommandHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot);
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
        assertThat(result2.stdout).contains(result.stdout);
    }

    @Test
    @Order(10)
    public void testCreateHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(11)
    public void testCreateAppHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(12)
    public void testCreateCliHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "cli", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(13)
    public void testCreateExtensionHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "extension", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(20)
    public void testBuildHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "build", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(30)
    public void testDevHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "dev", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
        assertThat(result.stdout).contains("--offline");
    }

    @Test
    @Order(40)
    public void testExtHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "extension", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(41)
    public void testExtCatHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "cat", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "ext", "cat", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(42)
    public void testExtListHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "ls", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "ext", "list", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(43)
    public void testExtAddHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "add", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(44)
    public void testExtRemoveHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "rm", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "ext", "remove", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(50)
    public void testRegistryHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "registry", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(51)
    public void testRegistryListHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "registry", "list", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(51)
    public void testRegistryAddHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "registry", "add", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(52)
    public void testRegistryRemoveHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "registry", "rm", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Order(60)
    @Test
    public void testGenerateCompletionHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "completion", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(70)
    public void testCommandVersion(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "version", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(80)
    public void testMessageFlags(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        MessageWriter writer = MessageWriter.debug();
        writer.error("error"); // has emoji
        writer.warn("warn"); // has emoji
        writer.info(MessageIcons.NOOP_ICON + " info");
        writer.info(MessageIcons.SUCCESS_ICON + " info");
        writer.info(MessageIcons.FAILURE_ICON + " info");
        writer.debug("debug");
    }

    @Test
    @Order(90)
    public void testImageHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
        assertTrue(result.getStdout().contains("Commands:"), "Should list subcommands\n");
        assertTrue(result.getStdout().contains("build"), "Should list build subcommand\n");
        assertTrue(result.getStdout().contains("push"), "Should list build subcommand\n");
    }

    @Test
    @Order(92)
    public void testImageBuildHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "build", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
        assertTrue(result.getStdout().contains("Commands:"), "Should list subcommands\n");
        assertTrue(result.getStdout().contains("docker"), "Should list docker subcommand\n");
        assertTrue(result.getStdout().contains("podman"), "Should list podman subcommand\n");
        assertTrue(result.getStdout().contains("jib"), "Should list jib subcommand\n");
        assertTrue(result.getStdout().contains("openshift"), "Should list openshift subcommand\n");
        assertTrue(result.getStdout().contains("buildpack"), "Should list buildpack subcommand\n");

    }

    @ParameterizedTest
    @Order(93)
    @ValueSource(strings = { "docker", "podman", "jib", "openshift", "buildpack" })
    public void testImageBuildBuilderHelp(String builder, QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "build", builder, "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(96)
    public void testImagePushHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "push", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
        assertThat(result.stdout).contains("--registry");
        assertThat(result.stdout).contains("--registry-username");
        assertThat(result.stdout).contains("--registry-password");
        assertThat(result.stdout).contains("--registry-password-stdin");
    }

    @ParameterizedTest
    @Order(97)
    @ValueSource(strings = { "docker", "podman", "jib", "openshift", "buildpack" })
    public void testImagePushBuilderHelp(String builder, QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "push", builder, "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(101)
    public void testDeployHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "deploy", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(102)
    public void testDeployKubernetesHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "deploy", "kubernetes", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
        assertThat(result.stdout).contains("--api-server-url");
        assertThat(result.stdout).contains("--token");
        assertThat(result.stdout).contains("--namespace");
        assertThat(result.stdout).contains("--deployment-kind");
    }

    @Test
    @Order(103)
    public void testDeployOpenshiftHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "deploy", "openshift", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
        assertThat(result.stdout).contains("--api-server-url");
        assertThat(result.stdout).contains("--token");
        assertThat(result.stdout).contains("--namespace");
        assertThat(result.stdout).contains("--deployment-kind");
    }

    @Test
    @Order(104)
    public void testDeployKnativeHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "deploy", "knative", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
        assertThat(result.stdout).contains("--api-server-url");
        assertThat(result.stdout).contains("--token");
        assertThat(result.stdout).contains("--namespace");
    }

    @Order(105)
    public void testPluginHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "plug", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "plugin", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(106)
    public void testPlugnListHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "plug", "list", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "plugin", "list", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(107)
    public void testPlugnAddHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "plug", "add", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "plugin", "add", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(108)
    public void testPlugnRemoveHelp(QuarkusMainLauncher launcher) throws Exception {
        CliDriver.setLauncher(launcher);
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "plug", "remove", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "plugin", "remove", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }
}
