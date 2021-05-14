package io.quarkus.cli;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * This is ordered to make output easier to view (as it effectively dumps help)
 */
@TestMethodOrder(OrderAnnotation.class)
public class CliHelpTest {

    @Test
    @Order(1)
    public void testCommandHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute("--help");
        result.echoSystemOut();
    }

    @Test
    @Order(20)
    public void testCreateHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute("create", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(21)
    public void testCreateAppHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute("create", "app", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(22)
    public void testCreateCliHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute("create", "cli", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(23)
    @Disabled
    public void testCreateExtensionHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute("create", "extension", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(30)
    public void testBuildHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute("build", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(40)
    public void testDevHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute("dev", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(50)
    public void testExtHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute("ext", "--help");
        result.echoSystemOut();

        CliDriver.Result result2 = CliDriver.execute("extension", "--help");
        Assertions.assertEquals(result.stdout, result2.stdout, "Help output for command aliases should be the same.");
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(51)
    public void testExtAddHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute("ext", "add", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(52)
    public void testExtListHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute("ext", "ls", "--help");
        result.echoSystemOut();

        CliDriver.Result result2 = CliDriver.execute("ext", "list", "--help");
        Assertions.assertEquals(result.stdout, result2.stdout, "Help output for command aliases should be the same.");
        CliDriver.println("-- same as above\n\n");
    }

    @Test
    @Order(53)
    public void testExtRemoveHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute("ext", "rm", "--help");
        result.echoSystemOut();

        CliDriver.Result result2 = CliDriver.execute("ext", "remove", "--help");
        Assertions.assertEquals(result.stdout, result2.stdout, "Help output for command aliases should be the same.");
        CliDriver.println("-- same as above\n\n");
    }

    @Order(60)
    @Test
    public void testGenerateCompletionHelp() throws Exception {
        CliDriver.Result result = CliDriver.execute("generate-completion", "--help");
        result.echoSystemOut();
    }

    @Test
    @Order(70)
    public void testCommandVersion() throws Exception {
        CliDriver.Result result = CliDriver.execute("version", "--help");
        result.echoSystemOut();
    }
}
