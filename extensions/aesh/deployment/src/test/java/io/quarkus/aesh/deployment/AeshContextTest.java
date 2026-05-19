package io.quarkus.aesh.deployment;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.AeshContext;
import io.quarkus.aesh.runtime.AeshMode;
import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that the {@link AeshContext} synthetic bean is produced correctly by the
 * {@link io.quarkus.aesh.runtime.AeshRecorder} and contains build-time command metadata.
 */
public class AeshContextTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    GitCommand.class,
                    CloneCommand.class,
                    StatusCommand.class));

    @Inject
    AeshContext aeshContext;

    @Test
    public void testAeshContextIsInjectable() {
        Assertions.assertThat(aeshContext).isNotNull();
    }

    @Test
    public void testAeshContextContainsCommands() {
        Assertions.assertThat(aeshContext.getCommands()).isNotEmpty();
        // Should have the GitCommand and StatusCommand (CloneCommand is a sub-command)
        Assertions.assertThat(aeshContext.getCommands().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    public void testAeshContextMode() {
        // With multiple commands + @CliCommand, should resolve to console mode
        AeshMode mode = aeshContext.getMode();
        Assertions.assertThat(mode).isNotNull();
        Assertions.assertThat(mode).isNotEqualTo(AeshMode.auto);
        // With @CliCommand and multiple commands, should be console mode
        Assertions.assertThat(mode).isEqualTo(AeshMode.console);
    }

    @Test
    public void testCommandMetadata() {
        var gitCmd = aeshContext.getCommands().stream()
                .filter(c -> "git".equals(c.getCommandName()))
                .findFirst();
        Assertions.assertThat(gitCmd).isPresent();
        Assertions.assertThat(gitCmd.get().isGroupCommand()).isTrue();
        Assertions.assertThat(gitCmd.get().getSubCommandClassNames())
                .contains(CloneCommand.class.getName());

        var statusCmd = aeshContext.getCommands().stream()
                .filter(c -> "status".equals(c.getCommandName()))
                .findFirst();
        Assertions.assertThat(statusCmd).isPresent();
        Assertions.assertThat(statusCmd.get().isGroupCommand()).isFalse();
    }

    @GroupCommandDefinition(name = "git", description = "Git-like commands", groupCommands = { CloneCommand.class })
    @CliCommand
    public static class GitCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "clone", description = "Clone a repository")
    public static class CloneCommand implements Command<CommandInvocation> {
        @Option(name = "url")
        private String url;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "status", description = "Show status")
    @CliCommand
    public static class StatusCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
