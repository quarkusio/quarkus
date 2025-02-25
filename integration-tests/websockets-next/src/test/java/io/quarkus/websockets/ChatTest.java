package io.quarkus.websockets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.ChatServer.ChatMessage;
import io.quarkus.websockets.ChatServer.MessageType;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.Json;

@QuarkusTest
public class ChatTest {

    @TestHTTPResource("/chat/Tom")
    URI uri;

    @Test
    public void testWebsocketChat() throws Exception {
        CountDownLatch messageLatch = new CountDownLatch(2);
        List<ChatMessage> messages = new CopyOnWriteArrayList<>();
        Vertx vertx = Vertx.vertx();
        WebSocketClient client = vertx.createWebSocketClient();
        try {
            client.connect(new WebSocketConnectOptions()
                    .setHost(uri.getHost())
                    .setPort(uri.getPort())
                    .setURI(uri.getPath()))
                    .onSuccess(
                            ws -> {
                                ws.textMessageHandler(m -> {
                                    messages.add(Json.decodeValue(m, ChatMessage.class));
                                    messageLatch.countDown();
                                });
                                ws.writeTextMessage(Json.encode(new ChatMessage(MessageType.CHAT_MESSAGE, "Tom", "Ping")));
                            });
            assertTrue(messageLatch.await(10, TimeUnit.SECONDS), messageLatch.toString());
            assertEquals(new ChatMessage(MessageType.USER_JOINED, "Tom", "Hello!"),
                    messages.get(0));
            assertEquals(new ChatMessage(MessageType.CHAT_MESSAGE, "Tom", "Ping"),
                    messages.get(1));
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            vertx.close();
        }
    }

}
