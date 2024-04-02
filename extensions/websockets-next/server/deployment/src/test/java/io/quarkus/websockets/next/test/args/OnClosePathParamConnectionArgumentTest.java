package io.quarkus.websockets.next.test.args;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketConnectOptions;

public class OnClosePathParamConnectionArgumentTest {

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
    void testArguments() throws InterruptedException {
        String header = "fool";
        WSClient client = WSClient.create(vertx).connect(new WebSocketConnectOptions().addHeader("X-Test", header), testUri);
        client.disconnect();
        assertTrue(MontyEcho.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertEquals("foo:monty:fool", MontyEcho.CLOSED_MESSAGE.get());
    }

    @WebSocket(path = "/echo/{grail}/and/{life}")
    public static class MontyEcho {

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);
        static final AtomicReference<String> CLOSED_MESSAGE = new AtomicReference<>();

        @OnOpen
        void open() {
        }

        @OnClose
        void close(@PathParam String life, @PathParam String grail, WebSocketConnection connection) {
            CLOSED_MESSAGE.set(life + ":" + grail + ":" + connection.handshakeRequest().header("X-Test"));
            CLOSED_LATCH.countDown();
        }

    }

}
