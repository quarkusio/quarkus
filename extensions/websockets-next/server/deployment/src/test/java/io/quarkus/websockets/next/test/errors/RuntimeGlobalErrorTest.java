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
import io.quarkus.websockets.next.BinaryEncodeException;
import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class RuntimeGlobalErrorTest {

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
        assertEquals("Global: Something went wrong", client.getLastMessage().toString());
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        @OnBinaryMessage
        void process(WebSocketConnection connection, Buffer message) {
            throw new IllegalStateException("Something went wrong");
        }

        @OnError
        String encodingError(BinaryEncodeException e) {
            return "Problem encoding: " + e.getEncodedObject().toString();
        }

        @OnError
        String decodingError(BinaryDecodeException e) {
            return "Problem decoding: " + e.getBytes().toString();
        }

        @OnError
        Uni<Void> runtimeProblem(RuntimeException e, WebSocketConnection connection) {
            return connection.sendText(e.getMessage());
        }

        @OnError
        String catchAll(Throwable e) {
            return "Ooops!";
        }

    }

    public static class GlobalErrorHandlers {

        @OnError
        Uni<Void> runtimeProblem(IllegalStateException e, WebSocketConnection connection) {
            assertTrue(Context.isOnEventLoopThread());
            return connection.sendText("Global: " + e.getMessage());
        }

    }

}
