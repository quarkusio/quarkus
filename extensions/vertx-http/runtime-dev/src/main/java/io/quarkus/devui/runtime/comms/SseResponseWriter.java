package io.quarkus.devui.runtime.comms;

import io.vertx.core.http.HttpServerResponse;

public class SseResponseWriter implements JsonRpcResponseWriter {
    private final HttpServerResponse response;

    public SseResponseWriter(HttpServerResponse response) {
        this.response = response;
    }

    @Override
    public void write(String message) {
        if (!response.closed()) {
            response.write("data: " + message + "\n\n");
        }
    }

    @Override
    public void close() {
        response.end();
    }

    @Override
    public boolean isOpen() {
        return !response.closed();
    }

    @Override
    public boolean isClosed() {
        return response.closed();
    }

}