package io.quarkus.it.keycloak;

import static io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager.getAccessToken;

import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.quarkus.websockets.BearerTokenClientEndpointConfigurator;

@QuarkusTest
@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
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
