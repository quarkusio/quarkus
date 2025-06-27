package io.quarkus.devui.runtime.comms;

import io.vertx.core.http.ServerWebSocket;

public class WsResponseWriter implements JsonRpcResponseWriter {
    private final ServerWebSocket socket;

    public WsResponseWriter(ServerWebSocket socket) {
        this.socket = socket;
    }

    @Override
    public void write(String message) {
        if (!socket.isClosed()) {
            socket.writeTextMessage(message);
        }
    }

    @Override
    public void close() {
        socket.close();
    }

    @Override
    public boolean isOpen() {
        return !socket.isClosed();
    }

    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }
}