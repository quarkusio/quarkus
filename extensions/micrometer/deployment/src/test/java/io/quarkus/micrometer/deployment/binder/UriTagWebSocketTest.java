package io.quarkus.micrometer.deployment.binder;

import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * The upgrade request of a {@code quarkus-websockets} endpoint must be tagged with the endpoint path template, not
 * with the concrete path.
 */
public class UriTagWebSocketTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.otel.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withApplicationRoot((jar) -> jar.addClasses(Util.class, GreetingWebSocket.class));

    @Inject
    MeterRegistry registry;

    @TestHTTPResource("ws")
    URI wsUri;

    @Test
    public void upgradeRequestsAreTaggedWithThePathTemplate() throws Exception {
        Assertions.assertEquals("hello alice", greet("alice"));
        Assertions.assertEquals("hello bob", greet("bob"));

        Util.waitForMeters(registry.find("http.server.requests").timers(), 1);
        System.out.println("Server paths\n" + Util.listMeters(registry, "http.server.requests"));

        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/ws/{name}").timers().size(),
                Util.foundServerRequests(registry, "The WebSocket endpoint template (/ws/{name}) should be used"));
        Assertions.assertEquals(0, registry.find("http.server.requests").tag("uri", "/ws/alice").timers().size(),
                Util.foundServerRequests(registry, "The concrete path (/ws/alice) should not be used"));
    }

    private String greet(String name) throws Exception {
        LinkedBlockingDeque<String> messages = new LinkedBlockingDeque<>();
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
        }, ClientEndpointConfig.Builder.create().build(), URI.create(wsUri + "/" + name));
        try {
            return messages.poll(20, TimeUnit.SECONDS);
        } finally {
            session.close();
        }
    }

    @ServerEndpoint("/ws/{name}")
    public static class GreetingWebSocket {

        @OnMessage
        String greet(String message, @PathParam("name") String name) {
            return message + " " + name;
        }
    }
}
