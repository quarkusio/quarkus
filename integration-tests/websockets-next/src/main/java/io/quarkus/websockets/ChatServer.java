package io.quarkus.websockets;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;

@WebSocket(path = "/chat/{username}")
public class ChatServer {

    public enum MessageType {
        USER_JOINED,
        USER_LEFT,
        CHAT_MESSAGE
    }

    @RegisterForReflection
    public record ChatMessage(MessageType type, String from, String message) {
    }

    @OnOpen(broadcast = true)
    public ChatMessage onOpen(@PathParam String username) {
        return new ChatMessage(MessageType.USER_JOINED, username, "Hello!");
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        connection.broadcast()
                .sendTextAndAwait(new ChatMessage(MessageType.USER_LEFT, connection.pathParam("username"), "Bye!"));
    }

    @OnTextMessage(broadcast = true)
    public ChatMessage onMessage(ChatMessage message) {
        return message;
    }

}
