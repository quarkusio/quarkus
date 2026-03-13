package io.quarkus.aesh.websocket.runtime;

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

    public AeshWebSocketConnection(WebSocketConnection ws) {
        this.ws = ws;
    }

    @Override
    protected void write(byte[] buffer) {
        ws.sendTextAndAwait(new String(buffer, outputEncoding()));
    }

    @Override
    public void close() {
        super.close();
        if (ws.isOpen()) {
            ws.closeAndAwait();
        }
    }
}
