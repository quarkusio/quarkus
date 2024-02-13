package io.quarkus.websockets.test;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * smoke test that websockets work as expected in dev mode
 */
public class WebsocketRootPathTestCase {

    @TestHTTPResource("echo")
    URI echoUri;

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(a -> {
                a.addClasses(EchoWebSocket.class, EchoService.class)
                        .add(new StringAsset("quarkus.http.root-path=/foo"), "application.properties");
            });

    @Test
    public void testHttpRootPath() throws Exception {

        LinkedBlockingDeque<String> message = new LinkedBlockingDeque<>();
        LinkedBlockingDeque<String> pongMessages = new LinkedBlockingDeque<>();
        Session session = ContainerProvider.getWebSocketContainer().connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig endpointConfig) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String s) {
                        message.add(s);
                    }
                });
                session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
                    @Override
                    public void onMessage(PongMessage s) {
                        ByteBuffer data = s.getApplicationData();
                        byte[] copy = new byte[data.remaining()];
                        data.get(copy);
                        pongMessages.add(new String(copy, StandardCharsets.UTF_8));
                    }
                });
                session.getAsyncRemote().sendText("hello");
                try {
                    session.getAsyncRemote().sendPing(ByteBuffer.wrap("PING".getBytes(StandardCharsets.UTF_8)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, ClientEndpointConfig.Builder.create().build(), echoUri);

        try {
            Assertions.assertEquals("hello", message.poll(20, TimeUnit.SECONDS));

            Assertions.assertEquals("PING", pongMessages.poll(20, TimeUnit.SECONDS));
        } finally {
            session.close();
        }
    }
}
