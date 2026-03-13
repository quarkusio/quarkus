package io.quarkus.aesh.ssh.runtime;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.Device;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.tty.Size;

/**
 * A {@link Connection} wrapper that tracks user input activity for idle timeout detection
 * and provides a close callback for connection lifecycle management.
 * <p>
 * Intercepts {@link #setStdinHandler(Consumer)} to wrap the handler with one that
 * updates {@link #lastActivityMs} on every keystroke. Intercepts {@link #setCloseHandler(Consumer)}
 * and {@link #close()} to ensure the {@code onClose} callback is always invoked exactly once.
 * All other methods delegate to the underlying connection.
 */
public class IdleTrackingConnection implements Connection {

    private final Connection delegate;
    private final Runnable onClose;
    private volatile long lastActivityMs;
    private final AtomicBoolean closed = new AtomicBoolean();

    public IdleTrackingConnection(Connection delegate, Runnable onClose) {
        this.delegate = delegate;
        this.onClose = onClose;
        this.lastActivityMs = System.currentTimeMillis();
    }

    /**
     * Returns the timestamp (in milliseconds) of the last input activity.
     */
    public long getLastActivityMs() {
        return lastActivityMs;
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
    public Consumer<Size> getSizeHandler() {
        return delegate.getSizeHandler();
    }

    @Override
    public void setSizeHandler(Consumer<Size> handler) {
        delegate.setSizeHandler(handler);
    }

    @Override
    public Consumer<Signal> getSignalHandler() {
        return delegate.getSignalHandler();
    }

    @Override
    public void setSignalHandler(Consumer<Signal> handler) {
        delegate.setSignalHandler(handler);
    }

    @Override
    public Consumer<int[]> getStdinHandler() {
        return delegate.getStdinHandler();
    }

    @Override
    public void setStdinHandler(Consumer<int[]> handler) {
        if (handler == null) {
            delegate.setStdinHandler(null);
        } else {
            delegate.setStdinHandler(data -> {
                lastActivityMs = System.currentTimeMillis();
                handler.accept(data);
            });
        }
    }

    @Override
    public Consumer<int[]> stdoutHandler() {
        return delegate.stdoutHandler();
    }

    @Override
    public void setCloseHandler(Consumer<Void> closeHandler) {
        // Chain: preserve any previously set handler and add the new one.
        // This ensures handlers set by AeshRemoteConnectionHandler are not
        // lost when AeshConsoleRunner later sets its own close handler.
        Consumer<Void> previous = delegate.getCloseHandler();
        delegate.setCloseHandler(v -> {
            runOnClose(); // idempotent due to closed flag
            if (previous != null) {
                previous.accept(v);
            }
            if (closeHandler != null) {
                closeHandler.accept(v);
            }
        });
    }

    @Override
    public Consumer<Void> getCloseHandler() {
        return delegate.getCloseHandler();
    }

    @Override
    public void close() {
        runOnClose();
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
    public Attributes getAttributes() {
        return delegate.getAttributes();
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

    private void runOnClose() {
        if (closed.compareAndSet(false, true)) {
            onClose.run();
        }
    }
}
