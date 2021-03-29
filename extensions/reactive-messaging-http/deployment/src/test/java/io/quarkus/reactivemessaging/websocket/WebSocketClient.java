package io.quarkus.reactivemessaging.websocket;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;

public class WebSocketClient {

    private final Vertx vertx;

    public WebSocketClient(Vertx vertx) {
        this.vertx = vertx;
    }

    public WsConnection connect(URI uri) {
        return connect(uri, 1, TimeUnit.SECONDS);
    }

    public WsConnection connect(URI uri, long timeout, TimeUnit unit) {
        CompletableFuture<WebSocket> webSocket = new CompletableFuture<>();
        vertx.createHttpClient().webSocket(uri.getPort(), uri.getHost(), uri.getPath(), ws -> {
            if (ws.succeeded()) {
                webSocket.complete(ws.result());
            } else {
                webSocket.completeExceptionally(ws.cause());
            }
        });
        try {
            return new WsConnection(webSocket.get(timeout, unit));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Web socket client failed", e);
        }
    }

    public static class WsConnection {

        private WebSocket ws;
        private List<String> responses = new CopyOnWriteArrayList<>();

        private WsConnection(WebSocket ws) {
            this.ws = ws;
            ws.handler(buffer -> responses.add(buffer.toString()));
        }

        public WsConnection send(String message) {
            try {
                ws.writeTextMessage(message);
            } catch (Exception any) {
                throw new RuntimeException("Failed to send message " + message, any);
            }
            return this;
        }

        public List<String> getResponses() {
            return responses;
        }
    }
}
