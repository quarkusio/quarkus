package io.quarkus.deployment.console;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ConsoleStateManagerTest {

    @Test
    public void testDuplicateKeyLogsWarningInsteadOfThrowing() {
        ConsoleStateManager manager = new ConsoleStateManager();
        ConsoleStateManager.ConsoleContext context1 = manager.createContext("Extension1");
        ConsoleStateManager.ConsoleContext context2 = manager.createContext("Extension2");

        ConsoleCommand command1 = new ConsoleCommand('a', "First command", null, () -> {
        });
        ConsoleCommand command2 = new ConsoleCommand('a', "Second command", null, () -> {
        });

        // First registration should succeed
        context1.addCommandInternal(command1);

        // Second registration with same key should log warning and skip, not throw
        context2.addCommandInternal(command2);

        // Verify that the first command is still registered
        ConsoleCommand registered = context1.getCommandByKey('a');
        assertThat(registered).isNotNull();
        assertThat(registered.getDescription()).isEqualTo("First command");
    }

    @Test
    public void testDifferentKeysCanCoexist() {
        ConsoleStateManager manager = new ConsoleStateManager();
        ConsoleStateManager.ConsoleContext context1 = manager.createContext("Extension1");
        ConsoleStateManager.ConsoleContext context2 = manager.createContext("Extension2");

        ConsoleCommand command1 = new ConsoleCommand('a', "First command", null, () -> {
        });
        ConsoleCommand command2 = new ConsoleCommand('b', "Second command", null, () -> {
        });

        context1.addCommandInternal(command1);
        context2.addCommandInternal(command2);

        // Both commands should be registered
        assertThat(context1.getCommandByKey('a')).isNotNull();
        assertThat(context2.getCommandByKey('b')).isNotNull();
    }
}
