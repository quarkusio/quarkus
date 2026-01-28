package io.quarkus.aesh.deployment;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.ParentCommand;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.TopCommand;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that parent-child command relationships work correctly
 * with {@code @GroupCommandDefinition} and {@code @ParentCommand}.
 */
public class ParentCommandSupportTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    ParentCmd.class,
                    ChildCmd.class))
            .setApplicationName("parent-cmd-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("child", "hello");

    @Test
    public void testParentChildCommandExecution() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .containsOnlyOnce("child: hello");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @GroupCommandDefinition(name = "parent", description = "Parent command", groupCommands = { ChildCmd.class })
    @TopCommand
    public static class ParentCmd implements Command<CommandInvocation> {

        @Option(shortName = 'v', name = "verbose", description = "Verbose output", hasValue = false)
        private boolean verbose;

        public boolean isVerbose() {
            return verbose;
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Use a subcommand");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "child", description = "Child command")
    public static class ChildCmd implements Command<CommandInvocation> {

        @ParentCommand
        private ParentCmd parent;

        @Argument(description = "The argument value")
        private String arg;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            String prefix = parent != null && parent.isVerbose() ? "[VERBOSE] " : "";
            invocation.println(prefix + "child: " + (arg != null ? arg : "none"));
            return CommandResult.SUCCESS;
        }
    }
}
