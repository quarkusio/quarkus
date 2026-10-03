package io.quarkus.websockets.test;

import java.net.URI;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.http.HttpServerRequest;

/**
 * The HTTP request of the handshake must be reachable from {@code ServerEndpointConfig.Configurator#modifyHandshake},
 * for example to read the remote address of the client.
 */
public class HandshakeRequestAccessTest {

    @TestHTTPResource("peer")
    URI peerUri;

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(a -> a.addClasses(PeerWebSocket.class, PeerConfigurator.class));

    @Test
    public void remoteAddressIsAvailableInModifyHandshake() throws Exception {
        LinkedBlockingDeque<String> messages = new LinkedBlockingDeque<>();
        AtomicReference<List<String>> peerHeader = new AtomicReference<>();
        ClientEndpointConfig clientConfig = ClientEndpointConfig.Builder.create()
                .configurator(new ClientEndpointConfig.Configurator() {
                    @Override
                    public void afterResponse(HandshakeResponse response) {
                        peerHeader.set(response.getHeaders().get("X-Peer"));
                    }
                }).build();
        Session session = ContainerProvider.getWebSocketContainer().connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig endpointConfig) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        messages.add(message);
                    }
                });
                session.getAsyncRemote().sendText("hello");
            }
        }, clientConfig, peerUri);
        try {
            Assertions.assertEquals("hello", messages.poll(20, TimeUnit.SECONDS));
            Assertions.assertEquals(List.of("127.0.0.1"), peerHeader.get());
        } finally {
            session.close();
        }
    }

    @ServerEndpoint(value = "/peer", configurator = PeerConfigurator.class)
    public static class PeerWebSocket {

        @OnMessage
        String echo(String message) {
            return message;
        }
    }

    public static class PeerConfigurator extends ServerEndpointConfig.Configurator {

        @Override
        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
            HttpServerRequest httpRequest = Arc.container().instance(HttpServerRequest.class).get();
            response.getHeaders().put("X-Peer", List.of(httpRequest.remoteAddress().host()));
        }
    }
}
