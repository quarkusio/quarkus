package io.quarkus.aesh.deployment;

import java.util.function.Consumer;

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
import io.quarkus.aesh.runtime.CliRunner;
import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that {@code quarkus.aesh.start-console=true} forces the local console
 * to start even when remote transports are present.
 * <p>
 * Since this is a QuarkusUnitTest (not prod mode), the CliRunner bean is registered
 * but not actually started. We verify that the CliRunner bean IS present in the
 * container, proving the config override works.
 */
public class RemoteTransportForceConsoleTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloCommand.class))
            .overrideConfigKey("quarkus.aesh.start-console", "true")
            .addBuildChainCustomizer(injectRemoteTransport());

    @Inject
    AeshContext aeshContext;

    @Inject
    CliRunner cliRunner;

    static Consumer<BuildChainBuilder> injectRemoteTransport() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new AeshRemoteTransportBuildItem("ssh"));
                    }
                }).produces(AeshRemoteTransportBuildItem.class).build();
            }
        };
    }

    @Test
    public void testCliRunnerIsRegistered() {
        // With start-console=true, CliRunner should be registered as a bean
        // even though a remote transport is present
        Assertions.assertThat(cliRunner).isNotNull();
    }

    @Test
    public void testModeIsConsole() {
        Assertions.assertThat(aeshContext.getMode()).isEqualTo(AeshMode.console);
    }

    @CommandDefinition(name = "hello", description = "Hello command")
    @CliCommand
    public static class HelloCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello");
            return CommandResult.SUCCESS;
        }
    }
}
