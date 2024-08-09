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

record Connection(URI uri, String[] messagesToSend, WSClient client, boolean broadcast, boolean binaryMode,
        String[] expectedResponses) {

    static Connection of(URI uri, boolean broadcast, boolean binaryMode, String[] sentMessages, String[] expectedResponses) {
        return new Connection(uri, sentMessages, null, broadcast, binaryMode, expectedResponses);
    }

    static Connection of(URI uri, String expectedResponse, boolean binaryMode, String... messages) {
        return new Connection(uri, messages, null, false, binaryMode, new String[] { expectedResponse });
    }

    private Connection with(WSClient client) {
        return new Connection(uri, messagesToSend, client, broadcast, binaryMode, expectedResponses);
    }

    private Set<String> getReceivedMessages() {
        return client.getMessages().stream().map(Buffer::toString).collect(Collectors.toSet());
    }

    static void sendAndAssertResponses(Vertx vertx, Connection... connections) {
        openConnectionsThenSend(connections, vertx, 0);
    }

    private static void openConnectionsThenSend(Connection[] connections, Vertx vertx, int idx) {
        var connection = connections[idx];
        final WSClient client = connection.binaryMode() ? new WSClient(vertx, BINARY) : new WSClient(vertx);
        try (client) {
            client.connect(new WebSocketConnectOptions(), connection.uri());
            connections[idx] = connection.with(client);

            if (idx < connections.length - 1) {
                openConnectionsThenSend(connections, vertx, idx + 1);
            } else {
                sendMessages(connections, connection.binaryMode());
            }
        }
    }

    private static void sendMessages(Connection[] connections, boolean binaryMode) {
        for (Connection connection : connections) {
            for (String message : connection.messagesToSend()) {
                if (binaryMode) {
                    connection.client().sendAndAwait(Buffer.buffer(message));
                } else {
                    connection.client().sendAndAwait(message);
                }
            }
            var expectedResponses = connection.expectedResponses();
            if (expectedResponses.length != 0) {
                if (connection.broadcast()) {
                    for (Connection conn : connections) {
                        assertResponses(conn, expectedResponses);
                    }
                } else {
                    assertResponses(connection, expectedResponses);
                }
            }
        }
    }

    private static void assertResponses(Connection connection, String[] expectedResponses) {
        connection.client.waitForMessages(expectedResponses.length);
        Set<String> actualResponses = connection.getReceivedMessages();

        for (String expectedResponse : expectedResponses) {
            assertTrue(actualResponses.contains(expectedResponse),
                    () -> "Expected response '%s' not found, was: %s".formatted(expectedResponse, actualResponses));
        }

        connection.client().getMessages().clear();
    }
}
