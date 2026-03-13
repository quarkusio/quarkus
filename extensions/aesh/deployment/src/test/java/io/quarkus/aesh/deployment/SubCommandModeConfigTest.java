package io.quarkus.aesh.deployment;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.CliConfig;
import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that all 6 {@link CliConfig.SubCommandModeConfig} properties bind correctly
 * from configuration overrides.
 */
public class SubCommandModeConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(DummyCommand.class))
            .overrideConfigKey("quarkus.aesh.sub-command-mode.enabled", "false")
            .overrideConfigKey("quarkus.aesh.sub-command-mode.exit-command", "quit")
            .overrideConfigKey("quarkus.aesh.sub-command-mode.alternative-exit-command", "back")
            .overrideConfigKey("quarkus.aesh.sub-command-mode.context-separator", "/")
            .overrideConfigKey("quarkus.aesh.sub-command-mode.show-context-on-entry", "false")
            .overrideConfigKey("quarkus.aesh.sub-command-mode.show-argument-in-prompt", "false");

    @Inject
    CliConfig cliConfig;

    @Test
    public void testSubCommandModeEnabled() {
        Assertions.assertThat(cliConfig.subCommandMode().enabled()).isFalse();
    }

    @Test
    public void testSubCommandModeExitCommand() {
        Assertions.assertThat(cliConfig.subCommandMode().exitCommand()).isEqualTo("quit");
    }

    @Test
    public void testSubCommandModeAlternativeExitCommand() {
        Assertions.assertThat(cliConfig.subCommandMode().alternativeExitCommand()).isEqualTo("back");
    }

    @Test
    public void testSubCommandModeContextSeparator() {
        Assertions.assertThat(cliConfig.subCommandMode().contextSeparator()).isEqualTo("/");
    }

    @Test
    public void testSubCommandModeShowContextOnEntry() {
        Assertions.assertThat(cliConfig.subCommandMode().showContextOnEntry()).isFalse();
    }

    @Test
    public void testSubCommandModeShowArgumentInPrompt() {
        Assertions.assertThat(cliConfig.subCommandMode().showArgumentInPrompt()).isFalse();
    }

    @CommandDefinition(name = "dummy", description = "Dummy command for config test")
    @CliCommand
    public static class DummyCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
