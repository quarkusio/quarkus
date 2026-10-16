package io.quarkus.aesh.deployment;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
        TestStubConnection stub = new TestStubConnection();
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
        TestStubConnection stub = new TestStubConnection();
        DelegatingConnection wrapper = new DelegatingConnection(stub) {
        };

        AtomicBoolean called = new AtomicBoolean(false);
        Consumer<Void> handler = v -> called.set(true);
        wrapper.setCloseHandler(handler);

        Assertions.assertThat(wrapper.closeHandler()).isSameAs(handler);
    }

    @Test
    public void testSetStdinHandlerDelegates() {
        TestStubConnection stub = new TestStubConnection();
        DelegatingConnection wrapper = new DelegatingConnection(stub) {
        };

        AtomicReference<int[]> received = new AtomicReference<>();
        Consumer<int[]> handler = received::set;
        wrapper.setStdinHandler(handler);

        Assertions.assertThat(wrapper.stdinHandler()).isSameAs(handler);
    }

    @Test
    public void testCloseDelegates() {
        TestStubConnection stub = new TestStubConnection();
        DelegatingConnection wrapper = new DelegatingConnection(stub) {
        };

        wrapper.close();
        Assertions.assertThat(stub.isClosed()).isTrue();
    }

    @Test
    public void testSetSizeHandlerDelegates() {
        TestStubConnection stub = new TestStubConnection();
        DelegatingConnection wrapper = new DelegatingConnection(stub) {
        };

        Consumer<Size> handler = s -> {
        };
        wrapper.setSizeHandler(handler);
        Assertions.assertThat(wrapper.sizeHandler()).isSameAs(handler);
    }

    @Test
    public void testSetSignalHandlerDelegates() {
        TestStubConnection stub = new TestStubConnection();
        DelegatingConnection wrapper = new DelegatingConnection(stub) {
        };

        Consumer<Signal> handler = s -> {
        };
        wrapper.setSignalHandler(handler);
        Assertions.assertThat(wrapper.signalHandler()).isSameAs(handler);
    }

}
