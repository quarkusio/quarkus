package io.quarkus.websockets.next.test.maxframesize;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.handler.codec.http.websocketx.CorruptedWebSocketFrameException;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketFrame;

public class MaxFrameSizeTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class, WSClient.class);
            })
            .overrideConfigKey("quarkus.websockets-next.server.max-frame-size", "10");

    @Inject
    Vertx vertx;

    @TestHTTPResource("/echo")
    URI echoUri;

    @Test
    void testMaxFrameSize() throws InterruptedException, ExecutionException, TimeoutException {
        WSClient client = WSClient.create(vertx).connect(echoUri);
        client.socket().writeFrame(WebSocketFrame.textFrame("foo".repeat(10), false));
        assertTrue(Echo.CORRUPTED_LATCH.await(5, TimeUnit.SECONDS));
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        static final CountDownLatch CORRUPTED_LATCH = new CountDownLatch(1);

        @OnTextMessage
        String process(String message) {
            return message;
        }

        @OnError
        void onError(CorruptedWebSocketFrameException e) {
            // Note that connection is automatically closed when CorruptedWebSocketFrameException is thrown
            CORRUPTED_LATCH.countDown();
        }

    }

}
