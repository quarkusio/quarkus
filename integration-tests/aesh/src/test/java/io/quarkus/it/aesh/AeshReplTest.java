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
 * auto-launches the REPL on first execute() and auto-closes
 * after each test method.
 */
@QuarkusMainTest
public class AeshReplTest {

    @Test
    void testHelloCommand(AeshLauncher launcher) {
        launcher.execute("hello --name=Alice");
        assertThat(launcher.getCommandOutput()).isEqualTo("Hello Alice!\n");
    }

    @Test
    void testHelloDefaultName(AeshLauncher launcher) {
        launcher.execute("hello");
        assertThat(launcher.getCommandOutput()).isEqualTo("Hello World!\n");
    }

    @Test
    void testMultipleCommandsInSession(AeshLauncher launcher) {
        launcher.execute("hello --name=First");
        launcher.execute("hello --name=Second");

        // Accumulated output contains both commands
        assertThat(launcher.getOutput()).isEqualTo("Hello First!\nHello Second!\n");

        // Last command output only
        assertThat(launcher.getCommandOutput()).isEqualTo("Hello Second!\n");
    }

    @Test
    void testGroupCommand(AeshLauncher launcher) {
        launcher.execute("cli version");
        assertThat(launcher.getCommandOutput()).isEqualTo("Version: 1.0.0\n");
    }

    @Test
    void testGroupCommandWithArgs(AeshLauncher launcher) {
        launcher.execute("cli run myTask");
        assertThat(launcher.getCommandOutput()).isEqualTo("Running task: myTask\n");
    }
}
