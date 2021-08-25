package io.quarkus.cli;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * This is ordered to make output easier to view (as it effectively dumps help)
 */
@TestMethodOrder(OrderAnnotation.class)
@QuarkusMainTest
public class CliHelpTest {

    @Test
    @Order(1)
    public void testCommandHelp(QuarkusMainLauncher launcher) throws Exception {
        var result = launcher.launch("--help");

        var result2 = launcher.launch("--help");
        Assertions.assertEquals(result.getOutputStream(), result2.getOutputStream(),
                "Invoking the base command should show usage help");
    }

    @Test
    @Order(20)
    @Launch({ "create", "--help" })
    public void testCreateHelp(LaunchResult result) throws Exception {
        result.echoSystemOut();
    }

    @Test
    @Order(21)
    @Launch({ "create", "app", "--help" })
    public void testCreateAppHelp(LaunchResult result) throws Exception {
        result.echoSystemOut();
    }

    @Test
    @Order(22)
    @Launch({ "create", "cli", "--help" })
    public void testCreateCliHelp(LaunchResult result) throws Exception {
        result.echoSystemOut();
    }

    @Test
    @Order(23)
    @Disabled
    @Launch({ "create", "extension", "--help" })
    public void testCreateExtensionHelp(LaunchResult result) throws Exception {
        result.echoSystemOut();
    }

    @Test
    @Order(30)
    @Launch({ "build", "--help" })
    public void testBuildHelp(LaunchResult result) throws Exception {
        result.echoSystemOut();
    }

    @Test
    @Order(40)
    @Launch({ "dev", "--help" })
    public void testDevHelp(LaunchResult result) throws Exception {
        result.echoSystemOut();
    }

    @Test
    @Order(50)
    public void testExtHelp(QuarkusMainLauncher launcher) throws Exception {
        var result = launcher.launch("ext", "--help");
        result.echoSystemOut();

        var result2 = launcher.launch("extension", "--help");
        Assertions.assertEquals(result.getOutput(), result2.getOutput(), "Help output for command aliases should be the same.");
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(51)
    @Launch({})
    public void testExtCatHelp(QuarkusMainLauncher launcher) throws Exception {
        var result = launcher.launch("ext", "cat", "--help");
        result.echoSystemOut();

        var result2 = launcher.launch("ext", "cat", "--help");
        Assertions.assertEquals(result.getOutput(), result2.getOutput(), "Help output for command aliases should be the same.");
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(52)
    public void testExtListHelp(QuarkusMainLauncher launcher) throws Exception {
        var result = launcher.launch("ext", "ls", "--help");
        result.echoSystemOut();

        var result2 = launcher.launch("ext", "list", "--help");
        Assertions.assertEquals(result.getOutput(), result2.getOutput(), "Help output for command aliases should be the same.");
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(53)
    @Launch({})
    public void testExtAddHelp(LaunchResult result) throws Exception {
        result.echoSystemOut();
    }

    @Test
    @Order(53)
    public void testExtRemoveHelp(QuarkusMainLauncher launcher) throws Exception {
        var result = launcher.launch("ext", "rm", "--help");
        result.echoSystemOut();

        var result2 = launcher.launch("ext", "remove", "--help");
        Assertions.assertEquals(result.getOutput(), result2.getOutput(), "Help output for command aliases should be the same.");
        CliDriver.println("-- same as above\n\n");
    }

    @Order(60)
    @Test
    @Launch({ "completion", "--help" })
    public void testGenerateCompletionHelp(LaunchResult result) throws Exception {
        result.echoSystemOut();
    }

    @Test
    @Order(70)
    @Launch({ "version", "--help" })
    public void testCommandVersion(LaunchResult result) throws Exception {
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
