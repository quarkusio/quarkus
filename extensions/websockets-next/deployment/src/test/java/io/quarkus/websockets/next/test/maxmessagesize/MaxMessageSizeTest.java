package io.quarkus.websockets.next.test.maxmessagesize;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

public class MaxMessageSizeTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class, WSClient.class);
            }).overrideConfigKey("quarkus.websockets-next.server.max-message-size", "10");

    @Inject
    Vertx vertx;

    @TestHTTPResource("/echo")
    URI echoUri;

    @Test
    void testMaxMessageSize() {
        WSClient client = WSClient.create(vertx).connect(echoUri);
        String msg = "foo".repeat(10);
        String reply = client.sendAndAwaitReply(msg).toString();
        assertNotEquals(msg, reply);
        assertTrue(Echo.ISE_THROWN.get());
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        static final AtomicBoolean ISE_THROWN = new AtomicBoolean();

        @OnTextMessage
        String process(String message) {
            return message;
        }

        @OnError
        String onError(IllegalStateException ise) {
            ISE_THROWN.set(true);
            return ise.getMessage();
        }

    }

}
