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
 * Tests that history-related configuration properties ({@code persistHistory},
 * {@code historyFile}, {@code historySize}) bind correctly to {@link CliConfig}.
 */
public class CliConfigHistorySettingsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(DummyCommand.class))
            .overrideConfigKey("quarkus.aesh.persist-history", "true")
            .overrideConfigKey("quarkus.aesh.history-file", "/tmp/test_aesh_history")
            .overrideConfigKey("quarkus.aesh.history-size", "1000");

    @Inject
    CliConfig cliConfig;

    @Test
    public void testPersistHistory() {
        Assertions.assertThat(cliConfig.persistHistory()).isTrue();
    }

    @Test
    public void testHistoryFile() {
        Assertions.assertThat(cliConfig.historyFile()).isPresent();
        Assertions.assertThat(cliConfig.historyFile().get()).isEqualTo("/tmp/test_aesh_history");
    }

    @Test
    public void testHistorySize() {
        Assertions.assertThat(cliConfig.historySize()).isEqualTo(1000);
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
