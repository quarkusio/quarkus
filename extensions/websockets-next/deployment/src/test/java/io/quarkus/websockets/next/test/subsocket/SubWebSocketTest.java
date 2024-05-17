package io.quarkus.websockets.next.test.subsocket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.test.subsocket.Sub.SubSub;
import io.quarkus.websockets.next.test.subsocket.Sub.SubSub.SubSubSub;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;

public class SubWebSocketTest {

    @Inject
    Vertx vertx;

    @TestHTTPResource("sub")
    URI echoUri;

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Sub.class, SubSub.class, SubSubSub.class);
            });

    @Test
    public void testSub() throws Exception {
        assertEcho(echoUri, "", "hello", "hello");
    }

    @Test
    public void testSubSub() throws Exception {
        assertEcho(echoUri, "/sub/1", "hello", "1:hello");
    }

    @Test
    public void testSubSubSub() throws Exception {
        assertEcho(echoUri, "/sub/1/sub/foo", "hello", "1:foo:hello");
    }

    public void assertEcho(URI testUri, String path, String payload, String expected) throws Exception {
        WebSocketClient client = vertx.createWebSocketClient();
        try {
            LinkedBlockingDeque<String> message = new LinkedBlockingDeque<>();
            client
                    .connect(testUri.getPort(), testUri.getHost(), testUri.getPath() + path)
                    .onComplete(r -> {
                        if (r.succeeded()) {
                            WebSocket ws = r.result();
                            ws.textMessageHandler(msg -> {
                                message.add(msg);
                            });
                            ws.writeTextMessage(payload);
                        } else {
                            throw new IllegalStateException(r.cause());
                        }
                    });
            assertEquals(expected, message.poll(10, TimeUnit.SECONDS));
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get();
        }
    }
}
