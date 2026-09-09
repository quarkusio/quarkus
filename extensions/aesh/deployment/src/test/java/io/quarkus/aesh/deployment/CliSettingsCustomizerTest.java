package io.quarkus.aesh.deployment;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.settings.SettingsBuilder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.AeshRemoteConnectionHandler;
import io.quarkus.aesh.runtime.CliSettings;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that a {@link CliSettings} SPI bean is discovered and invoked when
 * a remote connection is handled.
 */
public class CliSettingsCustomizerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    DummyCommand.class,
                    TestCliSettings.class,
                    TestStubConnection.class))
            .addBuildChainCustomizer(injectRemoteTransport());

    @Inject
    AeshRemoteConnectionHandler connectionHandler;

    static Consumer<BuildChainBuilder> injectRemoteTransport() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new AeshRemoteTransportBuildItem("test"));
                    }
                }).produces(AeshRemoteTransportBuildItem.class).build();
            }
        };
    }

    @Test
    public void testCliSettingsCustomizerIsInvoked() throws Exception {
        TestCliSettings.INVOKED.set(false);

        TestStubConnection connection = new TestStubConnection();

        Thread handler = new Thread(() -> connectionHandler.handle(connection, "test"));
        handler.setDaemon(true);
        handler.start();

        // Give some time for the connection handler to set up
        Thread.sleep(1000);

        // Close the connection
        connection.close();
        handler.join(10_000);

        Assertions.assertThat(TestCliSettings.INVOKED.get())
                .as("CliSettings.customize() should have been invoked")
                .isTrue();
    }

    @ApplicationScoped
    public static class TestCliSettings implements CliSettings {

        static final AtomicBoolean INVOKED = new AtomicBoolean(false);

        @Override
        public void customize(SettingsBuilder<?> builder) {
            INVOKED.set(true);
        }
    }

    @CommandDefinition(name = "dummy", description = "Dummy command")
    public static class DummyCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

}
