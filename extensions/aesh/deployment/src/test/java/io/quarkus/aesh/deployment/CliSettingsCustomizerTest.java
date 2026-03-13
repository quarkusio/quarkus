package io.quarkus.aesh.deployment;

import java.util.concurrent.CountDownLatch;
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
import io.quarkus.aesh.runtime.annotations.CliCommand;
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
                    TestCliSettings.class))
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

        StubConnection connection = new StubConnection();

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
        public void customize(SettingsBuilder<?, ?, ?, ?, ?, ?> builder) {
            INVOKED.set(true);
        }
    }

    @CommandDefinition(name = "dummy", description = "Dummy command")
    @CliCommand
    public static class DummyCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class StubConnection implements org.aesh.terminal.Connection {

        private final CountDownLatch closeLatch = new CountDownLatch(1);
        private volatile Consumer<Void> closeHandler;
        private volatile Consumer<int[]> stdinHandler;

        @Override
        public org.aesh.terminal.Device device() {
            return null;
        }

        @Override
        public org.aesh.terminal.tty.Size size() {
            return new org.aesh.terminal.tty.Size(80, 24);
        }

        @Override
        public Consumer<org.aesh.terminal.tty.Size> getSizeHandler() {
            return null;
        }

        @Override
        public void setSizeHandler(Consumer<org.aesh.terminal.tty.Size> handler) {
        }

        @Override
        public Consumer<org.aesh.terminal.tty.Signal> getSignalHandler() {
            return null;
        }

        @Override
        public void setSignalHandler(Consumer<org.aesh.terminal.tty.Signal> handler) {
        }

        @Override
        public Consumer<int[]> getStdinHandler() {
            return stdinHandler;
        }

        @Override
        public void setStdinHandler(Consumer<int[]> handler) {
            this.stdinHandler = handler;
        }

        @Override
        public Consumer<int[]> stdoutHandler() {
            return data -> {
            };
        }

        @Override
        public void setCloseHandler(Consumer<Void> handler) {
            this.closeHandler = handler;
        }

        @Override
        public Consumer<Void> getCloseHandler() {
            return closeHandler;
        }

        @Override
        public void close() {
            closeLatch.countDown();
            if (closeHandler != null) {
                closeHandler.accept(null);
            }
        }

        @Override
        public void openBlocking() {
            try {
                closeLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void openNonBlocking() {
        }

        @Override
        public boolean put(org.aesh.terminal.tty.Capability capability, Object... params) {
            return false;
        }

        @Override
        public org.aesh.terminal.Attributes getAttributes() {
            return new org.aesh.terminal.Attributes();
        }

        @Override
        public void setAttributes(org.aesh.terminal.Attributes attr) {
        }

        @Override
        public java.nio.charset.Charset inputEncoding() {
            return java.nio.charset.StandardCharsets.UTF_8;
        }

        @Override
        public java.nio.charset.Charset outputEncoding() {
            return java.nio.charset.StandardCharsets.UTF_8;
        }

        @Override
        public boolean supportsAnsi() {
            return true;
        }
    }
}
