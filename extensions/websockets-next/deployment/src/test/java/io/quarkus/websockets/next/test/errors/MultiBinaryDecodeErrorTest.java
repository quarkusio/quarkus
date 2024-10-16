package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.BinaryDecodeException;
import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.core.Context;

public class MultiBinaryDecodeErrorTest {

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
        client.send(Buffer.buffer("1"));
        client.waitForMessages(1);
        assertEquals("Problem decoding: 1", client.getLastMessage().toString());
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        @OnBinaryMessage
        Multi<Integer> process(Multi<Integer> messages) {
            return messages;
        }

        @OnError
        String decodingError(BinaryDecodeException e) {
            assertTrue(Context.isOnWorkerThread());
            return "Problem decoding: " + e.getBytes().toString();
        }

    }

}
