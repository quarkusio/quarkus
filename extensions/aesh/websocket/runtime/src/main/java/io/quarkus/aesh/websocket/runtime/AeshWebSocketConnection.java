package io.quarkus.aesh.websocket.runtime;

import java.util.Objects;

import org.aesh.terminal.http.HttpTtyConnection;

import io.quarkus.websockets.next.WebSocketConnection;

/**
 * Bridges a Quarkus {@link WebSocketConnection} to aesh-readline's {@link HttpTtyConnection}.
 * <p>
 * Extends {@link HttpTtyConnection} to reuse its JSON protocol handling
 * ({@code init}, {@code read}, {@code resize} actions), terminal capability detection,
 * terminal size management, and input decoding. Only overrides {@link #write(byte[])}
 * to send terminal output through the Quarkus WebSocket connection.
 */
public class AeshWebSocketConnection extends HttpTtyConnection {

    private final WebSocketConnection ws;
    private volatile long lastActivityMs;

    public AeshWebSocketConnection(WebSocketConnection ws) {
        this.ws = Objects.requireNonNull(ws, "ws must not be null");
        this.lastActivityMs = System.currentTimeMillis();
    }

    /**
     * Records that user activity occurred on this connection.
     */
    public void recordActivity() {
        lastActivityMs = System.currentTimeMillis();
    }

    /**
     * Returns the timestamp (in milliseconds) of the last user activity.
     */
    public long getLastActivityMs() {
        return lastActivityMs;
    }

    @Override
    protected void write(byte[] buffer) {
        try {
            ws.sendTextAndAwait(new String(buffer, outputEncoding()));
        } catch (Exception e) {
            // WebSocket may have been closed by the client; suppress to avoid
            // propagating through aesh's output handling
        }
    }

    @Override
    public void close() {
        // Close the WebSocket first to prevent super.close() from triggering
        // writes to an already-closing connection via the close handler chain
        if (ws.isOpen()) {
            ws.closeAndAwait();
        }
        super.close();
    }
}
