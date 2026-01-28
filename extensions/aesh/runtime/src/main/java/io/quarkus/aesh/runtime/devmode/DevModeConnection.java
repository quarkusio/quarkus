package io.quarkus.aesh.runtime.devmode;

import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.Device;
import org.aesh.terminal.tty.Capability;
import org.aesh.terminal.tty.Signal;
import org.aesh.terminal.tty.Size;

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
public class DevModeConnection implements Connection {

    private static final Set<DevModeConnection> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final String RELOAD_MESSAGE = "\r\n\u001b[33m[DEV] Source changes detected \u2014 reloading...\u001b[0m\r\n";

    private final Connection delegate;

    public DevModeConnection(Connection delegate) {
        this.delegate = delegate;
        ACTIVE.add(this);
    }

    /**
     * Write a reload notification to all active dev mode connections,
     * then clear the set so stale entries never block subsequent restarts.
     * Called from {@link AeshHotReplacementSetup} via a pre-restart step
     * before the application restarts.
     */
    static void notifyAllReloading() {
        // Drain the set atomically so no stale connections survive across restarts
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
    public Consumer<int[]> stdoutHandler() {
        return delegate.stdoutHandler();
    }

    @Override
    public void setCloseHandler(Consumer<Void> closeHandler) {
        // Always chain ACTIVE removal so stale entries are cleaned up
        // regardless of whether the server or aesh triggers the close.
        delegate.setCloseHandler(v -> {
            ACTIVE.remove(this);
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
        ACTIVE.remove(this);
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
}
