package io.quarkus.devui.runtime.comms;

import java.nio.charset.StandardCharsets;

import io.vertx.core.http.HttpServerResponse;

public class HttpResponseWriter implements JsonRpcResponseWriter {
    private final HttpServerResponse response;

    public HttpResponseWriter(HttpServerResponse response) {
        this.response = response;
    }

    @Override
    public void write(String message) {
        String output = message + "\n\n";
        int length = output.getBytes(StandardCharsets.UTF_8).length;

        System.out.println("<<<<<<< " + output);

        if (!response.closed()) {
            response.putHeader("Content-Type", "application/json")
                    .putHeader("Content-Length", String.valueOf(length))
                    .end(output);
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

    @Override
    public void accepted() {
        response.setStatusCode(202).end();
    }
}
