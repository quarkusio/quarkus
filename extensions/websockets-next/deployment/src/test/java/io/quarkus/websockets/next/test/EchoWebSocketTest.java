package io.quarkus.websockets.next.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class EchoWebSocketTest {

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo")
    URI echoUri;

    @TestHTTPResource("echo-blocking")
    URI echoBlockingUri;

    @TestHTTPResource("echo-blocking-await")
    URI echoBlockingAwaitUri;

    @TestHTTPResource("echo-json")
    URI echoJson;

    @TestHTTPResource("echo-json-array")
    URI echoJsonArray;

    @TestHTTPResource("echo-pojo")
    URI echoPojo;

    @TestHTTPResource("echo-blocking-pojo")
    URI echoBlockingPojo;

    @TestHTTPResource("echo-multi-consume")
    URI echoMultiConsume;

    @TestHTTPResource("echo-multi-produce")
    URI echoMultiProduce;

    @TestHTTPResource("echo-multi-bidi")
    URI echoMultiBidi;

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class, EchoBlocking.class, EchoBlockingAndAwait.class, EchoService.class, EchoJson.class,
                        EchoJsonArray.class, EchoPojo.class, EchoBlockingPojo.class, EchoMultiConsume.class,
                        EchoMultiProduce.class, EchoMultiBidi.class);
            });

    @Test
    public void testEcho() throws Exception {
        assertEcho(echoUri, "hello");
    }

    @Test
    public void testEchoBlocking() throws Exception {
        assertEcho(echoBlockingUri, "hello");
    }

    @Test
    public void testEchoBlockingAndAwait() throws Exception {
        assertEcho(echoBlockingAwaitUri, "hello");
    }

    @Test
    public void testEchoJson() throws Exception {
        assertEcho(echoJson, new JsonObject().put("msg", "hello").encode());
    }

    @Test
    public void testEchoJsonArray() throws Exception {
        assertEcho(echoJsonArray, new JsonArray().add(new JsonObject().put("msg", "hello")).encode());
    }

    @Test
    public void testEchoPojo() throws Exception {
        assertEcho(echoPojo, new JsonObject().put("msg", "hello").toString());
    }

    @Test
    public void testEchoBlockingPojo() throws Exception {
        assertEcho(echoBlockingPojo, new JsonObject().put("msg", "hello").toString());
    }

    @Test
    public void testEchoMultiConsume() throws Exception {
        assertEcho(echoMultiConsume, "hello", (ws, queue) -> {
            ws.textMessageHandler(msg -> {
                if ("subscribed".equals(msg)) {
                    ws.writeTextMessage("hello");
                } else {
                    queue.add(msg);
                }
            });
        });
    }

    @Test
    public void testEchoMultiProduce() throws Exception {
        assertEcho(echoMultiProduce, "hello");
    }

    @Test
    public void testEchoMultiBidi() throws Exception {
        assertEcho(echoMultiBidi, "hello");
    }

    public void assertEcho(URI testUri, String payload) throws Exception {
        assertEcho(testUri, payload, (ws, queue) -> {
            ws.textMessageHandler(msg -> {
                queue.add(msg);
            });
            ws.writeTextMessage(payload);
        });
    }

    public void assertEcho(URI testUri, String payload, BiConsumer<WebSocket, Queue<String>> action) throws Exception {
        WebSocketClient client = vertx.createWebSocketClient();
        try {
            LinkedBlockingDeque<String> message = new LinkedBlockingDeque<>();
            client
                    .connect(testUri.getPort(), testUri.getHost(), testUri.getPath())
                    .onComplete(r -> {
                        if (r.succeeded()) {
                            WebSocket ws = r.result();
                            action.accept(ws, message);
                        } else {
                            throw new IllegalStateException(r.cause());
                        }
                    });
            assertEquals(payload, message.poll(10, TimeUnit.SECONDS));
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get();
        }
    }
}
