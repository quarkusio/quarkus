package io.quarkus.aesh.deployment;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.AeshRemoteConnectionHandler;
import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that {@code quarkus.aesh.enable-alias=true} and
 * {@code quarkus.aesh.enable-export=true} make alias and export commands available.
 */
public class CliConfigEnableAliasAndExportTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(DummyCommand.class))
            .overrideConfigKey("quarkus.aesh.enable-alias", "true")
            .overrideConfigKey("quarkus.aesh.enable-export", "true")
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
    public void testAliasCommandAvailable() throws Exception {
        CopyOnWriteArrayList<String> output = new CopyOnWriteArrayList<>();
        CountDownLatch responseLatch = new CountDownLatch(1);

        CapturingStubConnection connection = new CapturingStubConnection(data -> {
            StringBuilder sb = new StringBuilder();
            for (int cp : data) {
                sb.appendCodePoint(cp);
            }
            String text = sb.toString();
            output.add(text);
            if (output.size() > 3) {
                responseLatch.countDown();
            }
        });

        Thread handler = new Thread(() -> connectionHandler.handle(connection, "test"));
        handler.setDaemon(true);
        handler.start();

        // Wait for prompt
        Thread.sleep(1000);

        // Send "alias" command
        Consumer<int[]> stdinHandler = connection.getStdinHandler();
        if (stdinHandler != null) {
            stdinHandler.accept("alias\r".chars().toArray());
        }

        boolean received = responseLatch.await(10, TimeUnit.SECONDS);
        connection.close();
        handler.join(10_000);

        String allOutput = String.join("", output);
        // alias command should not produce a "command not found" error
        Assertions.assertThat(allOutput).doesNotContain("command not found");
    }

    @CommandDefinition(name = "dummy", description = "Dummy command")
    @CliCommand
    public static class DummyCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    public static class CapturingStubConnection implements org.aesh.terminal.Connection {

        private final CountDownLatch closeLatch = new CountDownLatch(1);
        private volatile Consumer<Void> closeHandler;
        private volatile Consumer<int[]> stdinHandler;
        private final Consumer<int[]> stdoutCapture;

        public CapturingStubConnection(Consumer<int[]> stdoutCapture) {
            this.stdoutCapture = stdoutCapture;
        }

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
            return stdoutCapture;
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
