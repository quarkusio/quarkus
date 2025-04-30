package io.quarkus.websockets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.ChatServer.ChatMessage;
import io.quarkus.websockets.ChatServer.MessageType;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;

@QuarkusTest
public class ChatClientTest {

    private static final LinkedBlockingDeque<ChatMessage> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/")
    URI uri;

    @Inject
    WebSocketConnector<ChatClient> connector;

    @Test
    public void testWebsocketChat() throws Exception {
        WebSocketClientConnection connection = connector
                .baseUri(uri)
                .pathParam("username", "Tom")
                .connectAndAwait();
        assertEquals(new ChatMessage(MessageType.USER_JOINED, "Tom", "Hello!"), MESSAGES.poll(10, TimeUnit.SECONDS));
        connection.sendTextAndAwait(new ChatMessage(MessageType.CHAT_MESSAGE, "Tom", "Ping"));
        assertEquals(new ChatMessage(MessageType.CHAT_MESSAGE, "Tom", "Ping"), MESSAGES.poll(10, TimeUnit.SECONDS));
        connection.closeAndAwait();
    }

    @WebSocketClient(path = "/chat/{username}")
    public static class ChatClient {

        @OnTextMessage
        void message(ChatMessage message) {
            MESSAGES.add(message);
        }

    }

}
