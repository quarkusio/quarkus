package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.TextDecodeException;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.mutiny.core.Context;

public class TextDecodeErrorTest {

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
        client.send("not a json");
        client.waitForMessages(1);
        assertEquals("Problem decoding: not a json", client.getLastMessage().toString());
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        @OnTextMessage
        void process(Pojo pojo) {
        }

        @OnError
        String decodingError(TextDecodeException e) {
            assertTrue(Context.isOnWorkerThread());
            return "Problem decoding: " + e.getText();
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

    }

}
