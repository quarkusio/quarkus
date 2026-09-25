package io.quarkus.websockets.test.handshake;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

import org.junit.jupiter.api.Assertions;

/**
 * Connects to the handshake endpoint, sends one message and returns the echo together with the handshake response
 * headers.
 */
final class HandshakeClient {

    record Result(String echo, Map<String, List<String>> responseHeaders) {
    }

    static Result connectAndEcho(URI uri) throws Exception {
        LinkedBlockingDeque<String> messages = new LinkedBlockingDeque<>();
        AtomicReference<Map<String, List<String>>> headers = new AtomicReference<>();
        ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                .configurator(new ClientEndpointConfig.Configurator() {
                    @Override
                    public void afterResponse(HandshakeResponse response) {
                        headers.set(response.getHeaders());
                    }
                }).build();
        Session session = ContainerProvider.getWebSocketContainer().connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig endpointConfig) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String s) {
                        messages.add(s);
                    }
                });
                session.getAsyncRemote().sendText("hello");
            }
        }, config, uri);
        try {
            String echo = messages.poll(20, TimeUnit.SECONDS);
            Assertions.assertEquals("hello", echo);
            return new Result(echo, headers.get());
        } finally {
            session.close();
        }
    }
}
