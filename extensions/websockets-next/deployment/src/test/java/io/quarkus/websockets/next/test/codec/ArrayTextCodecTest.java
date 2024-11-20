package io.quarkus.websockets.next.test.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ArrayTextCodecTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Item.class, Endpont.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("end")
    URI testUri;

    @Test
    public void testCodec() throws Exception {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(testUri);
            client.waitForMessages(1);
            assertEquals(new JsonArray().add(new JsonObject().put("name", "Foo").put("count", 1)).toString(),
                    client.getMessages().get(0).toString());
        }
    }

    @WebSocket(path = "/end")
    public static class Endpont {

        // The default JsonTextMessageCodec is used
        @OnOpen
        Item[] open() {
            Item item = new Item();
            item.setName("Foo");
            item.setCount(1);
            return new Item[] { item };
        }

    }
}
