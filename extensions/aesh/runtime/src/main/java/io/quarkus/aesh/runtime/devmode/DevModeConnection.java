package io.quarkus.aesh.runtime.devmode;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.aesh.terminal.Connection;

import io.quarkus.aesh.runtime.DelegatingConnection;

/**
 * A {@link Connection} wrapper that triggers dev mode hot replacement scans
 * on user input.
 * <p>
 * Intercepts {@link #setStdinHandler(Consumer)} to wrap the handler with one
 * that fires an async scan on every keystroke. The scan is rate-limited (at most
 * once every two seconds) so the overhead is negligible. If source changes are
 * detected, the application restarts and all connections are dropped.
 * <p>
 * Also tracks active connections so a reload notification can be written
 * to all terminals before the restart drops them.
 * <p>
 * This wrapper is only applied in dev mode when a {@link AeshHotReplacementSetup}
 * has registered a scan callback.
 */
public class DevModeConnection extends DelegatingConnection {

    private static final Set<DevModeConnection> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final String RELOAD_MESSAGE = "\r\n\u001b[33m[DEV] Source changes detected \u2014 reloading...\u001b[0m\r\n";

    public DevModeConnection(Connection delegate) {
        super(delegate);
        ACTIVE.add(this);
    }

    /**
     * Write a reload notification to all active dev mode connections,
     * then clear the set so stale entries never block subsequent restarts.
     */
    static void notifyAllReloading() {
        Set<DevModeConnection> snapshot = Set.copyOf(ACTIVE);
        ACTIVE.clear();
        for (DevModeConnection conn : snapshot) {
            try {
                Consumer<int[]> out = conn.delegate.stdoutHandler();
                if (out != null) {
                    out.accept(RELOAD_MESSAGE.codePoints().toArray());
                }
            } catch (Exception ignored) {
                // Connection may already be closing
            }
        }
    }

    @Override
    public void setStdinHandler(Consumer<int[]> handler) {
        if (handler == null) {
            delegate.setStdinHandler(null);
        } else {
            delegate.setStdinHandler(data -> {
                AeshHotReplacementInterceptor.fireAsync();
                handler.accept(data);
            });
        }
    }

    @Override
    public void setCloseHandler(Consumer<Void> closeHandler) {
        Consumer<Void> previous = delegate.closeHandler();
        delegate.setCloseHandler(v -> {
            ACTIVE.remove(this);
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
        ACTIVE.remove(this);
        delegate.close();
    }
}
