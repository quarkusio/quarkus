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
import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that when a remote transport (e.g. WebSocket or SSH) is present,
 * the local console is NOT started by default. The application should start
 * as a normal Quarkus server, not take over stdin/stdout.
 */
public class RemoteTransportSkipsConsoleTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloCommand.class))
            .addBuildChainCustomizer(injectRemoteTransport());

    @Inject
    AeshContext aeshContext;

    static Consumer<BuildChainBuilder> injectRemoteTransport() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new AeshRemoteTransportBuildItem("websocket"));
                    }
                }).produces(AeshRemoteTransportBuildItem.class).build();
            }
        };
    }

    @Test
    public void testModeIsConsoleWhenRemoteTransportPresent() {
        // Remote transport forces console mode for interactive sessions
        Assertions.assertThat(aeshContext.getMode()).isEqualTo(AeshMode.console);
    }

    @Test
    public void testApplicationStartsNormally() {
        // If CliRunner was registered as QuarkusApplication, the test would
        // block on stdin or fail because there is no terminal. The fact that
        // this test runs and completes proves the local console was NOT started.
        Assertions.assertThat(aeshContext).isNotNull();
        Assertions.assertThat(aeshContext.getCommands()).isNotEmpty();
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
