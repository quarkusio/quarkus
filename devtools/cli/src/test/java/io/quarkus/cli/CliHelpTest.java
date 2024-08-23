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

/**
 * This is ordered to make output easier to view (as it effectively dumps help)
 */
@TestMethodOrder(OrderAnnotation.class)
public class CliHelpTest {
    static Path workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath().resolve("target/test-project");

    @Test
    @Order(1)
    public void testCommandHelp() throws Exception {
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
    public void testCreateHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(11)
    public void testCreateAppHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(12)
    public void testCreateCliHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "cli", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(13)
    public void testCreateExtensionHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "extension", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(20)
    public void testBuildHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "build", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(30)
    public void testDevHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "dev", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
        assertThat(result.stdout).contains("--offline");
    }

    @Test
    @Order(40)
    public void testExtHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "extension", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(41)
    public void testExtCatHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "cat", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "ext", "cat", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(42)
    public void testExtListHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "ls", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "ext", "list", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(43)
    public void testExtAddHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "add", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(44)
    public void testExtRemoveHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "rm", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "ext", "remove", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(50)
    public void testRegistryHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "registry", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(51)
    public void testRegistryListHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "registry", "list", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(51)
    public void testRegistryAddHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "registry", "add", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(52)
    public void testRegistryRemoveHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "registry", "rm", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Order(60)
    @Test
    public void testGenerateCompletionHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "completion", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(70)
    public void testCommandVersion() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "version", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(80)
    public void testMessageFlags() throws Exception {
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
    public void testImageHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
        assertTrue(result.getStdout().contains("Commands:"), "Should list subcommands\n");
        assertTrue(result.getStdout().contains("build"), "Should list build subcommand\n");
        assertTrue(result.getStdout().contains("push"), "Should list build subcommand\n");
    }

    @Test
    @Order(92)
    public void testImageBuildHelp() throws Exception {
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
    public void testImageBuildBuilderHelp(String builder) throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "build", builder, "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(96)
    public void testImagePushHelp() throws Exception {
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
    public void testImagePushBuilderHelp(String builder) throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "push", builder, "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(101)
    public void testDeployHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "deploy", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(102)
    public void testDeployKubernetesHelp() throws Exception {
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
    public void testDeployOpenshiftHelp() throws Exception {
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
    public void testDeployKnativeHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "deploy", "knative", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
        assertThat(result.stdout).contains("--api-server-url");
        assertThat(result.stdout).contains("--token");
        assertThat(result.stdout).contains("--namespace");
    }

    @Order(105)
    public void testPluginHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "plug", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "plugin", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(106)
    public void testPlugnListHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "plug", "list", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "plugin", "list", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(107)
    public void testPlugnAddHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "plug", "add", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "plugin", "add", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(108)
    public void testPlugnRemoveHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "plug", "remove", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "plugin", "remove", "--help");
        assertThat(result.stdout).isEqualTo(result2.stdout);
        CliDriver.println("-- same as above\n\n");
    }
}
