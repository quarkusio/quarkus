package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.net.URI;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.TextEncodeException;
import io.quarkus.websockets.next.TextMessageCodec;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Context;

public class TextEncodeErrorTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo")
    URI testUri;

    @Test
    void testError() {
        WSClient client = WSClient.create(vertx).connect(testUri);
        client.send(new JsonObject().put("name", "Fixa").encode());
        client.waitForMessages(1);
        assertEquals("java.lang.IllegalArgumentException:Fixa", client.getLastMessage().toString());
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        @OnTextMessage(outputCodec = BadCodec.class)
        Pojo process(Pojo pojo) {
            return pojo;
        }

        @OnError
        String encodingError(TextEncodeException e) {
            assertTrue(Context.isOnWorkerThread());
            return e.getCause().toString() + ":" + e.getEncodedObject().toString();
        }

    }

    @Priority(-1) // Let the JsonTextMessageCodec decode the pojo
    @Singleton
    public static class BadCodec implements TextMessageCodec<Pojo> {

        @Override
        public boolean supports(Type type) {
            return type.equals(Pojo.class);
        }

        @Override
        public String encode(Pojo value) {
            throw new IllegalArgumentException();
        }

        @Override
        public Pojo decode(Type type, String value) {
            throw new UnsupportedOperationException();
        }

    }

    public static class Pojo {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }

}
