package io.quarkus.websockets.next.test.utils;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ClientWebSocket;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

public class WSClient implements AutoCloseable {

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

    public WSClient connect(WebSocketConnectOptions options, URI url) {
        StringBuilder uri = new StringBuilder();
        uri.append(url.getPath());
        if (url.getQuery() != null) {
            uri.append("?").append(url.getQuery());
        }
        ClientWebSocket webSocket = client.webSocket();
        webSocket.handler(b -> messages.add(b));
        await(webSocket.connect(options.setPort(url.getPort()).setHost(url.getHost()).setURI(uri.toString())));
        var prev = socket.getAndSet(webSocket);
        if (prev != null) {
            messages.clear();
            await(prev.close());
        }
        return this;
    }

    public WSClient connect(URI url) {
        return connect(new WebSocketConnectOptions(), url);
    }

    public Future<Void> send(String message) {
        return socket.get().writeTextMessage(message);
    }

    public void sendAndAwait(String message) {
        await(send(message));
    }

    public Future<Void> send(Buffer message) {
        return socket.get().writeBinaryMessage(message);
    }

    public void sendAndAwait(Buffer message) {
        await(send(message));
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

    public void waitForMessages(int count) {
        Awaitility.await().until(() -> messages.size() >= count);
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
        sendAndAwait(message);
        Awaitility.await().until(() -> messages.size() > c);
        return messages.get(c);
    }

    public boolean isClosed() {
        return socket.get().isClosed();
    }

    @Override
    public void close() {
        disconnect();
    }

}
