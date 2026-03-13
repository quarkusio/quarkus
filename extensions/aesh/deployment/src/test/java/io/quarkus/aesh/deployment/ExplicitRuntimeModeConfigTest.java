package io.quarkus.aesh.deployment;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that {@code quarkus.aesh.mode=runtime} forces runtime mode.
 * Even with multiple commands that would auto-detect as console mode,
 * runtime mode runs the first command and exits.
 */
public class ExplicitRuntimeModeConfigTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClass(RuntimeCommand.class))
            .setApplicationName("runtime-mode-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.aesh.mode", "runtime")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("--greeting=RuntimeMode");

    @Test
    public void testRuntimeModeForced() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .containsOnlyOnce("RuntimeMode");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "rtcmd", description = "Runtime mode command")
    @CliCommand
    public static class RuntimeCommand implements Command<CommandInvocation> {

        @Option(name = "greeting", defaultValue = "Hello")
        private String greeting;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println(greeting);
            return CommandResult.SUCCESS;
        }
    }
}
