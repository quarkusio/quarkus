package io.quarkus.websockets.next.test.telemetry;

import static io.quarkus.websockets.next.test.telemetry.AbstractWebSocketsOnMessageTest.getMetrics;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.Direction.INBOUND;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.Direction.OUTBOUND;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

import org.awaitility.Awaitility;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public final class MetricsAsserter {

    private static final String CLIENT_CONNECTION_OPENED = "quarkus.websockets.client.connections.opened";
    private static final String SERVER_CONNECTION_OPENED = "quarkus.websockets.server.connections.opened";
    private static final String SERVER_CONNECTION_ON_OPEN_ERROR = "quarkus.websockets.server.connections.onopen.errors";
    private static final String CLIENT_CONNECTION_CLOSED = "quarkus.websockets.client.connections.closed";
    private static final String SERVER_CONNECTION_CLOSED = "quarkus.websockets.server.connections.closed";
    private static final String SERVER_ENDPOINT_COUNT_ERRORS = "quarkus.websockets.server.endpoint.count.errors";
    private static final String CLIENT_ENDPOINT_COUNT_ERRORS = "quarkus.websockets.client.endpoint.count.errors";
    private static final String SERVER_COUNT = "quarkus.websockets.server.count";
    private static final String SERVER_BYTES = "quarkus.websockets.server.bytes";
    private static final String CLIENT_COUNT = "quarkus.websockets.client.count";
    private static final String CLIENT_BYTES = "quarkus.websockets.client.bytes";

    public enum Direction {
        INBOUND,
        OUTBOUND
    }

    int serverSentCount;
    int serverReceivedCount;
    int serverReceivedCountBytes;
    int serverSentCountBytes;
    int clientReceivedCount;
    int clientSentCount;
    int clientSentCountBytes;
    int clientReceivedCountBytes;
    int clientErrorCount;
    int serverErrorCount;
    int clientConnectionOpenedCount;
    int serverConnectionOpenedCount;

    void assertTotalMetricsForAllPaths(int serverErrorsDelta, int clientErrorsDelta, int serverReceivedCountDelta,
            int serverReceivedCountBytesDelta, int serverSentCountBytesDelta, int clientSentCountDelta,
            int clientSentCountBytesDelta, int clientReceivedCountBytesDelta, int serverSentCountDelta,
            int clientReceivedCountDelta) {
        serverReceivedCount += serverReceivedCountDelta;
        serverReceivedCountBytes += serverReceivedCountBytesDelta;
        serverSentCount += serverSentCountDelta;
        serverSentCountBytes += serverSentCountBytesDelta;
        clientSentCount += clientSentCountDelta;
        clientSentCountBytes += clientSentCountBytesDelta;
        clientReceivedCount += clientReceivedCountDelta;
        clientReceivedCountBytes += clientReceivedCountBytesDelta;
        clientErrorCount += clientErrorsDelta;
        serverErrorCount += serverErrorsDelta;

        Awaitility.await().atMost(Duration.ofSeconds(12)).untilAsserted(() -> getMetrics()
                .body(assertServerConnectionOpenedTotal(serverConnectionOpenedCount))
                .body(assertClientConnectionOpenedTotal(clientConnectionOpenedCount))
                .body(assertServerErrorTotal(serverErrorCount))
                .body(assertClientErrorTotal(clientErrorCount))
                .body(assertClientMessagesCountReceived(clientReceivedCount))
                .body(assertClientMessagesCountBytesSent(clientSentCountBytes))
                .body(assertClientMessagesCountBytesReceived(clientReceivedCountBytes))
                .body(assertClientMessagesCountSent(clientSentCount))
                .body(assertServerMessagesCountBytesReceived(serverReceivedCountBytes))
                .body(assertServerMessagesCountBytesSent(serverSentCountBytes))
                .body(assertServerMessagesCountReceived(serverReceivedCount))
                .body(assertServerMessagesCountSent(serverSentCount)));
    }

    static Matcher<String> assertClientMessagesCountBytesSent(String path, int clientSentCountBytes) {
        return assertTotal(CLIENT_BYTES, clientSentCountBytes, path, OUTBOUND);
    }

    static Matcher<String> assertClientMessagesCountBytesReceived(String path, int clientReceivedCountBytes) {
        return assertTotal(CLIENT_BYTES, clientReceivedCountBytes, path, INBOUND);
    }

    static Matcher<String> assertClientMessagesCountSent(String path, int clientSentCount) {
        return assertTotal(CLIENT_COUNT, clientSentCount, path, OUTBOUND);
    }

    static Matcher<String> assertClientMessagesCountReceived(int clientSentCount) {
        return assertTotal(CLIENT_COUNT, clientSentCount, null, INBOUND);
    }

    static Matcher<String> assertClientMessagesCountReceived(String path, int clientSentCount) {
        return assertTotal(CLIENT_COUNT, clientSentCount, path, INBOUND);
    }

    static Matcher<String> assertServerMessagesCountSent(int serverReceivedCount) {
        return assertServerMessagesCountSent(null, serverReceivedCount);
    }

    static Matcher<String> assertServerMessagesCountSent(String path, int serverReceivedCount) {
        return assertTotal(SERVER_COUNT, serverReceivedCount, path, OUTBOUND);
    }

    static Matcher<String> assertServerMessagesCountReceived(String path, int serverReceivedCount) {
        return assertTotal(SERVER_COUNT, serverReceivedCount, path, INBOUND);
    }

    static Matcher<String> assertServerMessagesCountBytesSent(String path, int serverSentCountBytes) {
        return assertTotal(SERVER_BYTES, serverSentCountBytes, path, OUTBOUND);
    }

    static Matcher<String> assertServerMessagesCountBytesReceived(String path, int serverReceivedCountBytes) {
        return assertTotal(SERVER_BYTES, serverReceivedCountBytes, path, INBOUND);
    }

    static Matcher<String> assertServerErrorTotal(String path, int serverErrorCount) {
        return assertTotal(SERVER_ENDPOINT_COUNT_ERRORS, serverErrorCount, path, null);
    }

    static Matcher<String> assertClientErrorTotal(String path, int clientErrorCount) {
        return assertTotal(CLIENT_ENDPOINT_COUNT_ERRORS, clientErrorCount, path, null);
    }

    static Matcher<String> assertServerConnectionOpeningFailedTotal(String path, int serverConnectionOpeningFailedCount) {
        return assertTotal(SERVER_CONNECTION_ON_OPEN_ERROR, serverConnectionOpeningFailedCount, path, null);
    }

    static Matcher<String> assertServerConnectionOpenedTotal(int serverConnectionOpenedCount) {
        return assertServerConnectionOpenedTotal(null, serverConnectionOpenedCount);
    }

    static Matcher<String> assertClientConnectionOpenedTotal(int clientConnectionOpenedCount) {
        return assertClientConnectionOpenedTotal(null, clientConnectionOpenedCount);
    }

    static Matcher<String> assertClientMessagesCountBytesSent(int clientSentCountBytes) {
        return assertClientMessagesCountBytesSent(null, clientSentCountBytes);
    }

    static Matcher<String> assertClientMessagesCountBytesReceived(int clientReceivedCountBytes) {
        return assertClientMessagesCountBytesReceived(null, clientReceivedCountBytes);
    }

    static Matcher<String> assertClientMessagesCountSent(int clientSentCount) {
        return assertClientMessagesCountSent(null, clientSentCount);
    }

    static Matcher<String> assertServerMessagesCountReceived(int serverReceivedCount) {
        return assertServerMessagesCountReceived(null, serverReceivedCount);
    }

    static Matcher<String> assertServerMessagesCountBytesSent(int serverSentCountBytes) {
        return assertServerMessagesCountBytesSent(null, serverSentCountBytes);
    }

    static Matcher<String> assertServerMessagesCountBytesReceived(int serverReceivedCountBytes) {
        return assertServerMessagesCountBytesReceived(null, serverReceivedCountBytes);
    }

    static Matcher<String> assertServerErrorTotal(int serverErrorCount) {
        return assertServerErrorTotal(null, serverErrorCount);
    }

    static Matcher<String> assertClientErrorTotal(int clientErrorCount) {
        return assertClientErrorTotal(null, clientErrorCount);
    }

    static Matcher<String> assertServerConnectionOpenedTotal(String path, int serverConnectionOpenedCount) {
        return assertTotal(SERVER_CONNECTION_OPENED, serverConnectionOpenedCount, path, null);
    }

    static Matcher<String> assertClientConnectionOpenedTotal(String path, int clientConnectionOpenedCount) {
        return assertTotal(CLIENT_CONNECTION_OPENED, clientConnectionOpenedCount, path, null);
    }

    static Matcher<String> assertServerConnectionClosedTotal(String path, int serverConnectionClosedCount) {
        return assertTotal(SERVER_CONNECTION_CLOSED, serverConnectionClosedCount, path, null);
    }

    static Matcher<String> assertClientConnectionClosedTotal(String path, int clientConnectionClosedCount) {
        return assertTotal(CLIENT_CONNECTION_CLOSED, clientConnectionClosedCount, path, null);
    }

    private static Matcher<String> assertTotal(String metricKey, int expectedCount, String path, Direction direction) {
        var prometheusFormatKey = "%s_total".formatted(toPrometheusFormat(metricKey));
        return new BaseMatcher<>() {
            @Override
            public boolean matches(Object o) {
                if (o instanceof String str) {
                    var sameKeyMultipleTags = str
                            .lines()
                            .filter(l -> l.contains(prometheusFormatKey))
                            .filter(l -> path == null || l.contains(path)) // filter by path
                            .filter(l -> direction == null || l.contains(direction.toString()))
                            .map(String::trim)
                            .toList();
                    // quarkus_websockets_server_messages_count_received_total{<<some path tag>>} 2.0
                    // quarkus_websockets_server_messages_count_received_total{<<different path tag>>} 5.0
                    // = 7
                    var totalSum = sameKeyMultipleTags
                            .stream()
                            .map(l -> l.substring(l.lastIndexOf(" ")).trim())
                            .map(Double::parseDouble)
                            .map(Double::intValue)
                            .reduce(0, Integer::sum);
                    return totalSum == expectedCount;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(
                        "Key '%s' with value '%d' and direction '%s'".formatted(prometheusFormatKey, expectedCount, direction));
            }
        };
    }

    private static String toPrometheusFormat(String dottedMicrometerFormat) {
        return dottedMicrometerFormat.replace(".", "_").replace("-", "_");
    }

    static int stringToBytes(String... messages) {
        return Arrays.stream(messages).map(msg -> msg.getBytes(StandardCharsets.UTF_8)).map(s -> s.length).reduce(0,
                Integer::sum);
    }
}
