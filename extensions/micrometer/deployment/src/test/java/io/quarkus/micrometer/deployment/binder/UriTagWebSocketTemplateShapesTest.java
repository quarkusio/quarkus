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
import jakarta.websocket.server.ServerEndpoint;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * The shapes of endpoint path a WebSocket application can declare: no variable, several variables, a variable
 * followed by a literal segment, and a path carrying a query string.
 */
public class UriTagWebSocketTemplateShapesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.otel.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withApplicationRoot((jar) -> jar.addClasses(Util.class, StreamWebSocket.class, RoomUserWebSocket.class,
                    TopicWebSocket.class, QueryWebSocket.class));

    @Inject
    MeterRegistry registry;

    @TestHTTPResource("/")
    URI root;

    @Test
    public void staticPathIsTaggedAsItself() throws Exception {
        Assertions.assertEquals("ok", echo("api/v2/stream"));

        assertTaggedOnce("/api/v2/stream");
    }

    @Test
    public void severalPathVariablesAreAllTemplated() throws Exception {
        Assertions.assertEquals("ok", echo("rooms/lobby/users/alice"));
        Assertions.assertEquals("ok", echo("rooms/lounge/users/bob"));

        assertTaggedOnce("/rooms/{roomId}/users/{userId}");
        assertNotTagged("/rooms/lobby/users/alice");
        assertNotTagged("/rooms/lounge/users/bob");
    }

    @Test
    public void aLiteralSegmentAfterAVariableIsKept() throws Exception {
        Assertions.assertEquals("ok", echo("topics/orders/subscribe"));
        Assertions.assertEquals("ok", echo("topics/shipments/subscribe"));

        assertTaggedOnce("/topics/{topic}/subscribe");
        assertNotTagged("/topics/orders/subscribe");
    }

    @Test
    public void aQueryStringIsNotPartOfTheTemplate() throws Exception {
        Assertions.assertEquals("ok", echo("ws?room=lobby&user=alice"));
        Assertions.assertEquals("ok", echo("ws?room=lounge&user=bob"));

        assertTaggedOnce("/ws");
        assertNotTagged("/ws?room=lobby&user=alice");
    }

    private void assertTaggedOnce(String uri) throws InterruptedException {
        Util.waitForMeters(registry.find("http.server.requests").tag("uri", uri).timers(), 1);
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", uri).timers().size(),
                Util.foundServerRequests(registry, "Exactly one timer should be tagged uri=" + uri));
    }

    private void assertNotTagged(String uri) {
        Assertions.assertEquals(0, registry.find("http.server.requests").tag("uri", uri).timers().size(),
                Util.foundServerRequests(registry, "No timer should be tagged uri=" + uri));
    }

    private String echo(String path) throws Exception {
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
                session.getAsyncRemote().sendText("ping");
            }
        }, ClientEndpointConfig.Builder.create().build(), URI.create(root + path));
        try {
            return messages.poll(20, TimeUnit.SECONDS);
        } finally {
            session.close();
        }
    }

    @ServerEndpoint("/api/v2/stream")
    public static class StreamWebSocket {

        @OnMessage
        String echo(String message) {
            return "ok";
        }
    }

    @ServerEndpoint("/rooms/{roomId}/users/{userId}")
    public static class RoomUserWebSocket {

        @OnMessage
        String echo(String message) {
            return "ok";
        }
    }

    @ServerEndpoint("/topics/{topic}/subscribe")
    public static class TopicWebSocket {

        @OnMessage
        String echo(String message) {
            return "ok";
        }
    }

    @ServerEndpoint("/ws")
    public static class QueryWebSocket {

        @OnMessage
        String echo(String message) {
            return "ok";
        }
    }
}
