package io.quarkus.aesh.deployment;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.aesh.terminal.Attributes;
import org.aesh.terminal.BaseDevice;
import org.aesh.terminal.Connection;
import org.aesh.terminal.Device;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.tty.Size;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.aesh.runtime.DelegatingConnection;

/**
 * Tests that DelegatingConnection properly delegates all methods to the underlying connection.
 */
public class DelegatingConnectionTest {

    @Test
    public void testAllMethodsDelegate() {
        StubConnection stub = new StubConnection();
        // Create a concrete subclass (DelegatingConnection is abstract)
        DelegatingConnection wrapper = new DelegatingConnection(stub) {
        };

        // device
        Assertions.assertThat(wrapper.device()).isSameAs(stub.device());

        // size
        Assertions.assertThat(wrapper.size()).isEqualTo(stub.size());

        // encoding
        Assertions.assertThat(wrapper.inputEncoding()).isEqualTo(StandardCharsets.UTF_8);
        Assertions.assertThat(wrapper.outputEncoding()).isEqualTo(StandardCharsets.UTF_8);

        // supportsAnsi
        Assertions.assertThat(wrapper.supportsAnsi()).isTrue();

        // stdoutHandler
        Assertions.assertThat(wrapper.stdoutHandler()).isSameAs(stub.stdoutHandler());
    }

    @Test
    public void testSetCloseHandlerDelegates() {
        StubConnection stub = new StubConnection();
        DelegatingConnection wrapper = new DelegatingConnection(stub) {
        };

        AtomicBoolean called = new AtomicBoolean(false);
        Consumer<Void> handler = v -> called.set(true);
        wrapper.setCloseHandler(handler);

        Assertions.assertThat(wrapper.closeHandler()).isSameAs(handler);
    }

    @Test
    public void testSetStdinHandlerDelegates() {
        StubConnection stub = new StubConnection();
        DelegatingConnection wrapper = new DelegatingConnection(stub) {
        };

        AtomicReference<int[]> received = new AtomicReference<>();
        Consumer<int[]> handler = received::set;
        wrapper.setStdinHandler(handler);

        Assertions.assertThat(wrapper.stdinHandler()).isSameAs(handler);
    }

    @Test
    public void testCloseDelegates() {
        StubConnection stub = new StubConnection();
        DelegatingConnection wrapper = new DelegatingConnection(stub) {
        };

        wrapper.close();
        Assertions.assertThat(stub.closed).isTrue();
    }

    @Test
    public void testSetSizeHandlerDelegates() {
        StubConnection stub = new StubConnection();
        DelegatingConnection wrapper = new DelegatingConnection(stub) {
        };

        Consumer<Size> handler = s -> {
        };
        wrapper.setSizeHandler(handler);
        Assertions.assertThat(wrapper.sizeHandler()).isSameAs(handler);
    }

    @Test
    public void testSetSignalHandlerDelegates() {
        StubConnection stub = new StubConnection();
        DelegatingConnection wrapper = new DelegatingConnection(stub) {
        };

        Consumer<Signal> handler = s -> {
        };
        wrapper.setSignalHandler(handler);
        Assertions.assertThat(wrapper.signalHandler()).isSameAs(handler);
    }

    /**
     * Minimal Connection stub for testing delegation.
     */
    static class StubConnection implements Connection {
        boolean closed = false;
        private Consumer<Void> closeHandler;
        private Consumer<int[]> stdinHandler;
        private Consumer<Size> sizeHandler;
        private Consumer<Signal> signalHandler;
        private final Consumer<int[]> stdoutHandler = data -> {
        };
        private final Device device = new BaseDevice("test");

        @Override
        public Device device() {
            return device;
        }

        @Override
        public Size size() {
            return new Size(80, 24);
        }

        @Override
        public Consumer<Size> sizeHandler() {
            return sizeHandler;
        }

        @Override
        public void setSizeHandler(Consumer<Size> handler) {
            this.sizeHandler = handler;
        }

        @Override
        public Consumer<Signal> signalHandler() {
            return signalHandler;
        }

        @Override
        public void setSignalHandler(Consumer<Signal> handler) {
            this.signalHandler = handler;
        }

        @Override
        public Consumer<int[]> stdinHandler() {
            return stdinHandler;
        }

        @Override
        public void setStdinHandler(Consumer<int[]> handler) {
            this.stdinHandler = handler;
        }

        @Override
        public Consumer<int[]> stdoutHandler() {
            return stdoutHandler;
        }

        @Override
        public void setCloseHandler(Consumer<Void> closeHandler) {
            this.closeHandler = closeHandler;
        }

        @Override
        public Consumer<Void> closeHandler() {
            return closeHandler;
        }

        @Override
        public void close() {
            closed = true;
            if (closeHandler != null) {
                closeHandler.accept(null);
            }
        }

        @Override
        public void openBlocking() {
        }

        @Override
        public void openNonBlocking() {
        }

        @Override
        public boolean put(Capability capability, Object... params) {
            return false;
        }

        @Override
        public Attributes attributes() {
            return new Attributes();
        }

        @Override
        public void setAttributes(Attributes attr) {
        }

        @Override
        public Charset inputEncoding() {
            return StandardCharsets.UTF_8;
        }

        @Override
        public Charset outputEncoding() {
            return StandardCharsets.UTF_8;
        }

        @Override
        public boolean supportsAnsi() {
            return true;
        }
    }
}
