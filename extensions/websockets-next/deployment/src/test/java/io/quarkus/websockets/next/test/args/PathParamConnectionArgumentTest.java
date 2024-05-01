package io.quarkus.websockets.next.test.args;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketConnectOptions;

public class PathParamConnectionArgumentTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(MontyEcho.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo/monty/and/foo")
    URI testUri;

    @Test
    void testArguments() {
        String header = "fool";
        WSClient client = WSClient.create(vertx).connect(new WebSocketConnectOptions().addHeader("X-Test", header), testUri);
        assertEquals("foo:python:monty:fool", client.sendAndAwaitReply("python").toString());
    }

    @WebSocket(path = "/echo/{grail}/and/{life}")
    public static class MontyEcho {

        @OnTextMessage
        String process(@PathParam String life, @PathParam String grail, String message, WebSocketConnection connection)
                throws InterruptedException {
            return life + ":" + message + ":" + grail + ":" + connection.handshakeRequest().header("X-Test");
        }

    }

}
