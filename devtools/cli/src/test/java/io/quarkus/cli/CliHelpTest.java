package io.quarkus.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

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
        writer.info(MessageIcons.OK_ICON + " info");
        writer.info(MessageIcons.NOK_ICON + " info");
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
        assertTrue(result.getStdout().contains("jib"), "Should list jib subcommand\n");
        assertTrue(result.getStdout().contains("openshift"), "Should list openshift subcommand\n");
        assertTrue(result.getStdout().contains("buildpack"), "Should list buildpack subcommand\n");

    }

    @Test
    @Order(93)
    public void testImageBuildDockerHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "build", "docker", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(94)
    public void testImageBuildJibHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "build", "jib", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(95)
    public void testImageBuildOpenshiftHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "build", "openshift", "--help");
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

    @Test
    @Order(97)
    public void testImagePushDockerHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "push", "docker", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(98)
    public void testImagePushJibHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "push", "jib", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(99)
    public void testImagePushOpenshiftHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "push", "openshift", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }

    @Test
    @Order(99)
    public void testImagePushBuildpackHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "image", "push", "buildpack", "--help");
        result.echoSystemOut();
        assertThat(result.stdout).contains("Usage");
    }
}
