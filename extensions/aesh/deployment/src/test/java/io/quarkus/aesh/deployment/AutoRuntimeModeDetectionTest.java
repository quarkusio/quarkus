package io.quarkus.aesh.deployment;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.AeshContext;
import io.quarkus.aesh.runtime.AeshMode;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that {@code quarkus.aesh.mode=auto} (the default) with exactly one
 * root command resolves to runtime mode and sets the top command class name.
 * No explicit mode override — relies on auto-detection.
 */
public class AutoRuntimeModeDetectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(SingleCommand.class))
            .overrideConfigKey("quarkus.aesh.mode", "auto");

    @Inject
    AeshContext aeshContext;

    @Test
    public void testSingleCommandAutoDetectsRuntimeMode() {
        Assertions.assertThat(aeshContext.getMode())
                .as("Single command should auto-detect as runtime mode")
                .isEqualTo(AeshMode.runtime);
    }

    @Test
    public void testTopCommandClassNameSet() {
        Assertions.assertThat(aeshContext.getTopCommandClassName())
                .as("Top command class name should be set in runtime mode")
                .isEqualTo(SingleCommand.class.getName());
    }

    @CommandDefinition(name = "single", description = "A single command")
    public static class SingleCommand implements Command<CommandInvocation> {

        @Option(name = "name", defaultValue = "World")
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello " + name + "!");
            return CommandResult.SUCCESS;
        }
    }
}
