package io.quarkus.cli;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot);
        Assertions.assertEquals(result.stdout, result2.stdout, "Invoking the base command should show usage help");
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(20)
    public void testCreateHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(21)
    public void testCreateAppHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(22)
    public void testCreateCliHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "cli", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(23)
    @Disabled
    public void testCreateExtensionHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "extension", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(30)
    public void testBuildHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "build", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(40)
    public void testDevHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "dev", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(50)
    public void testExtHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "--help");
        result.echoSystemOut();

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "extension", "--help");
        Assertions.assertEquals(result.stdout, result2.stdout, "Help output for command aliases should be the same.");
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(51)
    public void testExtCatHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "cat", "--help");
        result.echoSystemOut();

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "ext", "cat", "--help");
        Assertions.assertEquals(result.stdout, result2.stdout, "Help output for command aliases should be the same.");
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(52)
    public void testExtListHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "ls", "--help");
        result.echoSystemOut();

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "ext", "list", "--help");
        Assertions.assertEquals(result.stdout, result2.stdout, "Help output for command aliases should be the same.");
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(53)
    public void testExtAddHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "add", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(53)
    public void testExtRemoveHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "ext", "rm", "--help");
        result.echoSystemOut();

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "ext", "remove", "--help");
        Assertions.assertEquals(result.stdout, result2.stdout, "Help output for command aliases should be the same.");
        CliDriver.println("-- same as above\n\n");
    }

    @Order(60)
    @Test
    public void testGenerateCompletionHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "completion", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(70)
    public void testCommandVersion() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "version", "--help");
        result.echoSystemOut();
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
}
