package io.quarkus.websockets.next.test.onopenreturntypes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.quarkus.websockets.next.test.utils.WSClient.ReceiverMode;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class OnOpenReturnTypesTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(EndpointText.class, EndpointBinary.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("end-text")
    URI endText;

    @TestHTTPResource("end-binary")
    URI endBinary;

    @Test
    void testReturnTypes() throws Exception {
        try (WSClient textClient = WSClient.create(vertx, ReceiverMode.TEXT).connect(endText)) {
            textClient.waitForMessages(1);
            assertEquals("/end-text", textClient.getMessages().get(0).toString());
        }
        try (WSClient binaryClient = WSClient.create(vertx, ReceiverMode.BINARY).connect(endBinary)) {
            binaryClient.waitForMessages(1);
            assertEquals("/end-binary", binaryClient.getMessages().get(0).toString());
        }
    }

    @WebSocket(path = "/end-text")
    public static class EndpointText {

        @OnOpen
        String open(WebSocketConnection connection) {
            return connection.handshakeRequest().path();
        }

    }

    @WebSocket(path = "/end-binary")
    public static class EndpointBinary {

        @OnOpen
        Buffer open(WebSocketConnection connection) {
            return Buffer.buffer(connection.handshakeRequest().path());
        }

    }

}
