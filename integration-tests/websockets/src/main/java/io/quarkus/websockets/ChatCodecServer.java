package io.quarkus.websockets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/codec", encoders = ChatMessageEncoder.class, decoders = ChatMessageDecoder.class)
@ApplicationScoped
public class ChatCodecServer {

    Collection<Session> sessions = new ArrayList<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        broadcast("Session " + session.getId() + " closed");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session);
        broadcast("Session " + session.getId() + " closed on error: " + throwable);
    }

    @OnMessage
    public void onMessage(ChatMessageDTO message) {
        broadcast(String.format("%s in message [%s] said: %s", message.getFrom(), message.getId(), message.getContent()));
    }

    private void broadcast(String message) {
        sessions.forEach(s -> {
            ChatMessageDTO chatMessageDTO = new ChatMessageDTO();
            chatMessageDTO.setId(UUID.randomUUID().toString());
            chatMessageDTO.setFrom("SuperCoolWebsocket");
            chatMessageDTO.setContent(message);
            s.getAsyncRemote().sendObject(chatMessageDTO, result -> {
                if (result.getException() != null) {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }

}
