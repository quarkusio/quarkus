package io.quarkus.devui.runtime.js;

import io.quarkus.devui.runtime.comms.JsonRpcResponseWriter;
import io.quarkus.devui.runtime.comms.MessageType;
import io.vertx.core.http.ServerWebSocket;

public class JavaScriptResponseWriter implements JsonRpcResponseWriter {
    private final ServerWebSocket socket;

    public JavaScriptResponseWriter(ServerWebSocket socket) {
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

    @Override
    public Object decorateObject(Object object, MessageType messageType) {
        return new Result(messageType.name(), object);
    }
}