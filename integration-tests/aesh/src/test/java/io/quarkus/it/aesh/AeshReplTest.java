package io.quarkus.it.aesh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.aesh.AeshLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * Tests REPL (console) mode using AeshLauncher.
 * <p>
 * Uses the commands from src/main/java (hello, cli) which auto-detect
 * as console mode (multiple independent top-level commands). AeshLauncher
 * provides a test connection to send commands and assert on output without
 * needing a real terminal.
 */
@QuarkusMainTest
public class AeshReplTest {

    @Test
    void testHelloCommand(AeshLauncher launcher) {
        launcher.launch();

        String output = launcher.executeCommand("hello --name=Alice");
        assertThat(output).contains("Hello Alice!");

        launcher.exit();
    }

    @Test
    void testHelloDefaultName(AeshLauncher launcher) {
        launcher.launch();

        String output = launcher.executeCommand("hello");
        assertThat(output).contains("Hello World!");

        launcher.exit();
    }

    @Test
    void testMultipleCommandsInSession(AeshLauncher launcher) {
        launcher.launch();

        String out1 = launcher.executeCommand("hello --name=First");
        assertThat(out1).contains("Hello First!");

        String out2 = launcher.executeCommand("hello --name=Second");
        assertThat(out2).contains("Hello Second!");

        launcher.exit();
    }

    @Test
    void testGroupCommand(AeshLauncher launcher) {
        launcher.launch();

        String output = launcher.executeCommand("cli version");
        assertThat(output).contains("Version: 1.0.0");

        launcher.exit();
    }

    @Test
    void testGroupCommandWithArgs(AeshLauncher launcher) {
        launcher.launch();

        String output = launcher.executeCommand("cli run myTask");
        assertThat(output).contains("Running task: myTask");

        launcher.exit();
    }
}
