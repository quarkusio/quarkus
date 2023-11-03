package io.quarkus.websockets;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.json.bind.JsonbBuilder;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ChatCodecTest {

    private static final LinkedBlockingDeque<ChatMessageDTO> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/codec")
    URI uri;

    @Test
    public void testWebsocketChat() throws Exception {
        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri)) {
            ChatMessageDTO chatMessageDTO = new ChatMessageDTO();
            chatMessageDTO.setId(UUID.randomUUID().toString());
            chatMessageDTO.setFrom("SuperCoolProgrammer");
            chatMessageDTO.setContent("Hello my young padawan!");
            session.getAsyncRemote().sendText(JsonbBuilder.create().toJson(chatMessageDTO));
            Assertions.assertEquals(String.format("%s in message [%s] said: %s", chatMessageDTO.getFrom(),
                    chatMessageDTO.getId(), chatMessageDTO.getContent()), MESSAGES.poll(10, TimeUnit.SECONDS).getContent());
        }
    }

    @ClientEndpoint(encoders = ChatMessageEncoder.class, decoders = ChatMessageDecoder.class)
    public static class Client {

        @OnMessage
        void message(ChatMessageDTO msg) {
            MESSAGES.add(msg);
        }

    }

}
