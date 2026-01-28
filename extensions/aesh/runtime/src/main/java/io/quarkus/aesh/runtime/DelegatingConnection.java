package io.quarkus.aesh.runtime;

import java.nio.charset.Charset;
import java.util.function.Consumer;

import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.Device;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.tty.Size;

/**
 * Base {@link Connection} wrapper that delegates all methods to an underlying connection.
 * Subclasses override only the methods they need to intercept.
 */
public abstract class DelegatingConnection implements Connection {

    protected final Connection delegate;

    protected DelegatingConnection(Connection delegate) {
        this.delegate = delegate;
    }

    @Override
    public Device device() {
        return delegate.device();
    }

    @Override
    public Size size() {
        return delegate.size();
    }

    @Override
    public Consumer<Size> sizeHandler() {
        return delegate.sizeHandler();
    }

    @Override
    public void setSizeHandler(Consumer<Size> handler) {
        delegate.setSizeHandler(handler);
    }

    @Override
    public Consumer<Signal> signalHandler() {
        return delegate.signalHandler();
    }

    @Override
    public void setSignalHandler(Consumer<Signal> handler) {
        delegate.setSignalHandler(handler);
    }

    @Override
    public Consumer<int[]> stdinHandler() {
        return delegate.stdinHandler();
    }

    @Override
    public void setStdinHandler(Consumer<int[]> handler) {
        delegate.setStdinHandler(handler);
    }

    @Override
    public Consumer<int[]> stdoutHandler() {
        return delegate.stdoutHandler();
    }

    @Override
    public void setCloseHandler(Consumer<Void> closeHandler) {
        delegate.setCloseHandler(closeHandler);
    }

    @Override
    public Consumer<Void> closeHandler() {
        return delegate.closeHandler();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void openBlocking() {
        delegate.openBlocking();
    }

    @Override
    public void openNonBlocking() {
        delegate.openNonBlocking();
    }

    @Override
    public boolean put(Capability capability, Object... params) {
        return delegate.put(capability, params);
    }

    @Override
    public Attributes attributes() {
        return delegate.attributes();
    }

    @Override
    public void setAttributes(Attributes attr) {
        delegate.setAttributes(attr);
    }

    @Override
    public Charset inputEncoding() {
        return delegate.inputEncoding();
    }

    @Override
    public Charset outputEncoding() {
        return delegate.outputEncoding();
    }

    @Override
    public boolean supportsAnsi() {
        return delegate.supportsAnsi();
    }
}
