package io.quarkus.aesh.ssh.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that a warning is logged when the SSH server starts without
 * any authentication configured (no password, no authorized-keys-file).
 */
public class AeshSshNoAuthWarningTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCommand.class,
                    GoodbyeCommand.class))
            .overrideConfigKey("quarkus.aesh.ssh.port", "12227")
            .setLogRecordPredicate(record -> record.getLoggerName().contains("SshServerLifecycle"))
            .assertLogRecords(records -> {
                assertThat(records)
                        .as("Expected a warning about missing SSH authentication")
                        .anySatisfy(record -> {
                            assertThat(record.getLevel()).isEqualTo(Level.WARNING);
                            assertThat(record.getMessage()).contains("running without authentication");
                        });
            });

    @Test
    public void testWarningIsLogged() {
        // The assertion is done via assertLogRecords above.
        // This test just needs to exist so the extension starts up.
    }

    @CommandDefinition(name = "hello", description = "Say hello")
    @CliCommand
    public static class HelloCommand implements Command<CommandInvocation> {

        @Option(shortName = 'n', name = "name", defaultValue = "World")
        private String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello " + name + "!");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "goodbye", description = "Say goodbye")
    @CliCommand
    public static class GoodbyeCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Goodbye!");
            return CommandResult.SUCCESS;
        }
    }
}
