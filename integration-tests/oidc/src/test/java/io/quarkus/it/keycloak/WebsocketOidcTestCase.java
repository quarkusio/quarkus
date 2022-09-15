package io.quarkus.it.keycloak;

import static io.quarkus.it.keycloak.KeycloakXTestResourceLifecycleManager.getAccessToken;

import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.BearerTokenClientEndpointConfigurator;

@QuarkusTest
@QuarkusTestResource(KeycloakXTestResourceLifecycleManager.class)
public class WebsocketOidcTestCase {

    @TestHTTPResource("secured-hello")
    URI wsUri;

    @Test
    public void websocketTest() throws Exception {

        LinkedBlockingDeque<String> message = new LinkedBlockingDeque<>();
        Session session = ContainerProvider.getWebSocketContainer().connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig endpointConfig) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String s) {
                        message.add(s);
                    }
                });
                session.getAsyncRemote().sendText("hello");
            }
        }, new BearerTokenClientEndpointConfigurator(getAccessToken("alice")), wsUri);

        try {
            Assertions.assertEquals("hello alice@gmail.com", message.poll(20, TimeUnit.SECONDS));
        } finally {
            session.close();
        }
    }

}
