package io.quarkus.websockets.next.test.utils;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;

public class WSClient {

    private final WebSocketClient client;
    private AtomicReference<WebSocket> socket = new AtomicReference<>();
    private List<Buffer> messages = new CopyOnWriteArrayList<>();

    public WSClient(Vertx vertx) {
        this.client = vertx.createWebSocketClient();
    }

    public static WSClient create(Vertx vertx) {
        return new WSClient(vertx);
    }

    public static URI toWS(URI uri, String path) {
        String result = "ws://";
        if (uri.getScheme().equals("https")) {
            result = "wss://";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        result += uri.getHost() + ":" + uri.getPort() + "/" + path;
        try {
            return new URI(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public WSClient connect(URI url) {
        WebSocket webSocket = await(client.connect(url.getPort(), url.getHost(), url.getPath()));
        var prev = socket.getAndSet(webSocket);
        if (prev != null) {
            messages.clear();
            await(prev.close());
        }
        webSocket.handler(b -> messages.add(b));
        return this;
    }

    public void send(String message) {
        WebSocket current = socket.get();
        if (current != null) {
            await(current.writeTextMessage(message));
        }
    }

    public void send(Buffer message) {
        WebSocket current = socket.get();
        if (current != null) {
            await(current.writeBinaryMessage(message));
        }
    }

    public List<Buffer> getMessages() {
        return messages;
    }

    public Buffer getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    public Buffer waitForNextMessage() {
        var c = messages.size();
        Awaitility.await().until(() -> messages.size() > c);
        return messages.get(c);
    }

    public void disconnect() {
        WebSocket current = socket.getAndSet(null);
        if (current != null) {
            await(current.close());
        }
        messages.clear();
    }

    private <T> T await(Future<T> future) {
        return future.toCompletionStage().toCompletableFuture().join();
    }

    public Buffer sendAndAwaitReply(String message) {
        var c = messages.size();
        send(message);
        Awaitility.await().until(() -> messages.size() > c);
        return messages.get(c);
    }
}
