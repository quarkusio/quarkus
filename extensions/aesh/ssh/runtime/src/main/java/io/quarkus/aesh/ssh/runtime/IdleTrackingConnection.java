package io.quarkus.aesh.ssh.runtime;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.aesh.terminal.Connection;

import io.quarkus.aesh.runtime.DelegatingConnection;

/**
 * A {@link Connection} wrapper that tracks user input activity for idle timeout detection
 * and provides a close callback for connection lifecycle management.
 * <p>
 * Intercepts {@link #setStdinHandler(Consumer)} to wrap the handler with one that
 * updates {@link #lastActivityMs} on every keystroke. Intercepts {@link #setCloseHandler(Consumer)}
 * and {@link #close()} to ensure the {@code onClose} callback is always invoked exactly once.
 */
public class IdleTrackingConnection extends DelegatingConnection {

    private final Runnable onClose;
    private volatile long lastActivityMs;
    private final AtomicBoolean closed = new AtomicBoolean();

    public IdleTrackingConnection(Connection delegate, Runnable onClose) {
        super(delegate);
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
    public void setCloseHandler(Consumer<Void> closeHandler) {
        // Chain: preserve any previously set handler and add the new one.
        // This ensures handlers set by AeshRemoteConnectionHandler are not
        // lost when AeshConsoleRunner later sets its own close handler.
        Consumer<Void> previous = delegate.closeHandler();
        delegate.setCloseHandler(v -> {
            runOnClose();
            if (previous != null) {
                previous.accept(v);
            }
            if (closeHandler != null) {
                closeHandler.accept(v);
            }
        });
    }

    @Override
    public void close() {
        runOnClose();
        delegate.close();
    }

    private void runOnClose() {
        if (closed.compareAndSet(false, true)) {
            onClose.run();
        }
    }
}
