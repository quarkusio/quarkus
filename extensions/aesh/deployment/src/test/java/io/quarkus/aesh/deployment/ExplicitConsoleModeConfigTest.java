package io.quarkus.aesh.deployment;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.AeshContext;
import io.quarkus.aesh.runtime.AeshMode;
import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that {@code quarkus.aesh.mode=console} forces console mode even when
 * auto-detection would normally choose runtime mode (single command).
 */
public class ExplicitConsoleModeConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(SingleCommand.class))
            .overrideConfigKey("quarkus.aesh.mode", "console");

    @Inject
    AeshContext aeshContext;

    @Test
    public void testConsoleModeForced() {
        Assertions.assertThat(aeshContext).isNotNull();
        Assertions.assertThat(aeshContext.getMode()).isEqualTo(AeshMode.console);
    }

    @CommandDefinition(name = "single", description = "A single command")
    @CliCommand
    public static class SingleCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello");
            return CommandResult.SUCCESS;
        }
    }
}
