package io.quarkus.aesh.deployment;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.AeshRemoteConnectionHandler;
import io.quarkus.aesh.runtime.AeshSessionEvent;
import io.quarkus.aesh.runtime.SessionClosed;
import io.quarkus.aesh.runtime.SessionOpened;
import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that session lifecycle CDI events are fired when connections
 * are opened and closed.
 */
public class AeshSessionEventsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCommand.class,
                    SessionObserver.class))
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
    public void testSessionEventsAreFired() throws Exception {
        // Reset latches
        SessionObserver.reset();

        // Create a mock connection that closes immediately
        StubConnection connection = new StubConnection();

        // Handle the connection on a separate thread (it blocks)
        Thread handler = new Thread(() -> connectionHandler.handle(connection, "test"));
        handler.setDaemon(true);
        handler.start();

        // Wait for the opened event
        boolean opened = SessionObserver.OPEN_LATCH.await(10, TimeUnit.SECONDS);
        Assertions.assertThat(opened).as("SessionOpened event should be fired").isTrue();

        AeshSessionEvent openEvent = SessionObserver.OPEN_EVENT.get();
        Assertions.assertThat(openEvent).isNotNull();
        Assertions.assertThat(openEvent.sessionId()).isNotNull().isNotEmpty();
        Assertions.assertThat(openEvent.transport()).isEqualTo("test");
        Assertions.assertThat(openEvent.timestamp()).isNotNull();

        // Close the connection to trigger the closed event
        connection.close();
        handler.join(10_000);

        boolean closed = SessionObserver.CLOSE_LATCH.await(10, TimeUnit.SECONDS);
        Assertions.assertThat(closed).as("SessionClosed event should be fired").isTrue();

        AeshSessionEvent closeEvent = SessionObserver.CLOSE_EVENT.get();
        Assertions.assertThat(closeEvent).isNotNull();
        Assertions.assertThat(closeEvent.sessionId()).isEqualTo(openEvent.sessionId());
        Assertions.assertThat(closeEvent.transport()).isEqualTo("test");
    }

    @Singleton
    public static class SessionObserver {

        static volatile CountDownLatch OPEN_LATCH = new CountDownLatch(1);
        static volatile CountDownLatch CLOSE_LATCH = new CountDownLatch(1);
        static final AtomicReference<AeshSessionEvent> OPEN_EVENT = new AtomicReference<>();
        static final AtomicReference<AeshSessionEvent> CLOSE_EVENT = new AtomicReference<>();

        static void reset() {
            OPEN_LATCH = new CountDownLatch(1);
            CLOSE_LATCH = new CountDownLatch(1);
            OPEN_EVENT.set(null);
            CLOSE_EVENT.set(null);
        }

        void onOpen(@ObservesAsync @SessionOpened AeshSessionEvent event) {
            OPEN_EVENT.set(event);
            OPEN_LATCH.countDown();
        }

        void onClose(@ObservesAsync @SessionClosed AeshSessionEvent event) {
            CLOSE_EVENT.set(event);
            CLOSE_LATCH.countDown();
        }
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

    /**
     * Minimal Connection stub for testing session events.
     * The connection blocks on openBlocking() until close() is called.
     */
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
