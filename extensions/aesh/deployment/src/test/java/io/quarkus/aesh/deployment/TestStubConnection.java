package io.quarkus.aesh.deployment;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.Device;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.tty.Size;

/**
 * Minimal stub {@link Connection} for unit tests.
 * Provides a controllable connection that blocks on {@link #openBlocking()}
 * until {@link #close()} is called.
 */
class TestStubConnection implements Connection {

    private final CountDownLatch closeLatch = new CountDownLatch(1);
    private volatile boolean closed;
    private volatile Consumer<Void> closeHandler;
    private volatile Consumer<int[]> stdinHandler;
    private volatile Consumer<Signal> signalHandler;
    private volatile Consumer<Size> sizeHandler;

    @Override
    public Device device() {
        return null;
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
        return data -> {
        };
    }

    @Override
    public void setCloseHandler(Consumer<Void> handler) {
        this.closeHandler = handler;
    }

    @Override
    public Consumer<Void> closeHandler() {
        return closeHandler;
    }

    @Override
    public void close() {
        closed = true;
        closeLatch.countDown();
        if (closeHandler != null) {
            closeHandler.accept(null);
        }
    }

    public boolean isClosed() {
        return closed;
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
