package io.quarkus.websockets.next.test.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

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

public class TextInputCodecTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(FindInputCodec.class, FindInputCodec.MyInputCodec.class, AbstractFind.class, Item.class);
            });

    @TestHTTPResource("find-input-codec")
    URI itemCodecUri;

    @Inject
    Vertx vertx;

    @Test
    public void testCodec() throws Exception {
        JsonArray items = new JsonArray();
        items.add(new JsonObject().put("name", "foo").put("count", 10));
        items.add(new JsonObject().put("name", "bar").put("count", 1));
        items.add(new JsonObject().put("name", "baz").put("count", 100));
        assertCodec(itemCodecUri, items.encode(), "Item [count=2]");
    }

    public void assertCodec(URI testUri, String payload, String expected)
            throws Exception {
        WebSocketClient client = vertx.createWebSocketClient();
        try {
            LinkedBlockingDeque<String> message = new LinkedBlockingDeque<>();
            client
                    .connect(testUri.getPort(), testUri.getHost(), testUri.getPath())
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
