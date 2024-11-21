package io.quarkus.websockets.next.test.telemetry;

import static io.quarkus.websockets.next.test.utils.WSClient.ReceiverMode.BINARY;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocketConnectOptions;

record Connection(URI uri, String[] messagesToSend, boolean binaryMode,
        String[] expectedResponses) {

    static Connection of(URI uri, boolean binaryMode, String[] sentMessages, String[] expectedResponses) {
        return new Connection(uri, sentMessages, binaryMode, expectedResponses);
    }

    static Connection of(URI uri, String expectedResponse, boolean binaryMode, String... messages) {
        return new Connection(uri, messages, binaryMode, new String[] { expectedResponse });
    }

    void openConnectionThenSend(Vertx vertx) {
        final WSClient client = binaryMode() ? new WSClient(vertx, BINARY) : new WSClient(vertx);
        try (client) {
            client.connect(new WebSocketConnectOptions(), uri());
            for (String message : messagesToSend()) {
                if (binaryMode()) {
                    client.sendAndAwait(Buffer.buffer(message));
                } else {
                    client.sendAndAwait(message);
                }
            }
            var expectedResponses = expectedResponses();
            if (expectedResponses.length != 0) {
                client.waitForMessages(expectedResponses.length);
                Set<String> actualResponses = client.getMessages().stream().map(Buffer::toString).collect(Collectors.toSet());

                for (String expectedResponse : expectedResponses) {
                    assertTrue(actualResponses.contains(expectedResponse),
                            () -> "Expected response '%s' not found, was: %s".formatted(expectedResponse, actualResponses));
                }

                client.getMessages().clear();
            }
        }
    }
}
