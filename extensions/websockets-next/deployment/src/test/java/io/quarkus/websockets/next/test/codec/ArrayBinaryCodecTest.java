package io.quarkus.websockets.next.test.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.net.URI;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.BinaryMessageCodec;
import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class ArrayBinaryCodecTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Item.class, Endpoint.class, WSClient.class, ItemArrayBinaryCodec.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("end")
    URI testUri;

    @Test
    public void testCodec() {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(testUri);
            client.sendAndAwait(Buffer.buffer("Foo"));
            client.waitForMessages(1);
            assertEquals("Foo", client.getMessages().get(0).toString());
        }
    }

    @Singleton
    public static class ItemArrayBinaryCodec implements BinaryMessageCodec<Item[]> {

        @Override
        public boolean supports(Type type) {
            return (type instanceof GenericArrayType) || (type instanceof Class<?> && ((Class<?>) type).isArray());
        }

        @Override
        public Buffer encode(Item[] value) {
            return Buffer.buffer(value[0].getName());
        }

        @Override
        public Item[] decode(Type type, Buffer value) {
            throw new UnsupportedOperationException();
        }

    }

    @WebSocket(path = "/end")
    public static class Endpoint {

        @OnBinaryMessage
        Item[] process(String name) {
            Item item = new Item();
            item.setName(name);
            item.setCount(1);
            return new Item[] { item };
        }

    }
}
