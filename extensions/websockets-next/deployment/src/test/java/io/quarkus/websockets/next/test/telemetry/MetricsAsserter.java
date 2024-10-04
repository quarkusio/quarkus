package io.quarkus.websockets.next.test.telemetry;

import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_CONNECTION_CLOSED;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_CONNECTION_OPENED;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_MESSAGES_COUNT_ERRORS;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_MESSAGES_COUNT_RECEIVED_BYTES;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_MESSAGES_COUNT_SENT;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_MESSAGES_COUNT_SENT_BYTES;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.SERVER_CONNECTION_CLOSED;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.SERVER_CONNECTION_OPENED;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.SERVER_CONNECTION_OPENED_ERROR;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.SERVER_MESSAGES_COUNT_ERRORS;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.SERVER_MESSAGES_COUNT_RECEIVED;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.SERVER_MESSAGES_COUNT_RECEIVED_BYTES;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.SERVER_MESSAGES_COUNT_SENT_BYTES;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Consumer;

import org.awaitility.Awaitility;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

public final class MetricsAsserter {

    int serverReceivedCount;
    int serverReceivedCountBytes;
    int serverSentCountBytes;
    int clientSentCount;
    int clientSentCountBytes;
    int clientReceivedCountBytes;
    int clientErrorCount;
    int serverErrorCount;
    int clientConnectionOpenedCount;
    int serverConnectionOpenedCount;

    void assertMetrics(int serverErrorsDelta, int serverReceivedCountDelta, Connection connection) {
        int serverSentCountBytesDelta = connectionToSentBytes(connection);
        int serverReceivedCountBytesDelta = connectionToReceivedBytes(connection);
        assertMetrics(serverErrorsDelta, 0, serverReceivedCountDelta, serverReceivedCountBytesDelta,
                serverSentCountBytesDelta, 0, 0, 0);
    }

    void assertMetrics(int serverErrorsDelta, int serverReceivedCountDelta, int serverSentCountBytesDelta,
            int serverReceivedCountBytesDelta) {
        assertMetrics(serverErrorsDelta, 0, serverReceivedCountDelta, serverReceivedCountBytesDelta,
                serverSentCountBytesDelta, 0, 0, 0);
    }

    private int connectionToReceivedBytes(Connection connection) {
        return stringToBytes(connection.messagesToSend());
    }

    private int connectionToSentBytes(Connection connection) {
        return stringToBytes(connection.expectedResponses());
    }

    void assertMetrics(int serverErrorsDelta, int clientErrorsDelta, int serverReceivedCountDelta,
            int serverReceivedCountBytesDelta, int serverSentCountBytesDelta, int clientSentCountDelta,
            int clientSentCountBytesDelta, int clientReceivedCountBytesDelta) {
        addDeltasToTotalsMeasuredPreviously(serverErrorsDelta, clientErrorsDelta, serverReceivedCountDelta,
                serverReceivedCountBytesDelta, serverSentCountBytesDelta, clientSentCountDelta, clientSentCountBytesDelta,
                clientReceivedCountBytesDelta);

        assertMetrics(metrics -> metrics
                .body(assertServerConnectionOpenedTotal(serverConnectionOpenedCount))
                .body(assertClientConnectionOpenedTotal(clientConnectionOpenedCount))
                .body(assertServerErrorTotal(serverErrorCount))
                .body(assertClientErrorTotal(clientErrorCount))
                .body(assertClientMessagesCountBytesSent(clientSentCountBytes))
                .body(assertClientMessagesCountBytesReceived(clientReceivedCountBytes))
                .body(assertClientMessagesCountSent(clientSentCount))
                .body(assertServerMessagesCountBytesReceived(serverReceivedCountBytes))
                .body(assertServerMessagesCountBytesSent(serverSentCountBytes))
                .body(assertServerMessagesCountReceived(serverReceivedCount)));
    }

    private void addDeltasToTotalsMeasuredPreviously(int serverErrorsDelta, int clientErrorsDelta, int serverReceivedCountDelta,
            int serverReceivedCountBytesDelta, int serverSentCountBytesDelta, int clientSentCountDelta,
            int clientSentCountBytesDelta, int clientReceivedCountBytesDelta) {
        serverReceivedCount += serverReceivedCountDelta;
        serverReceivedCountBytes += serverReceivedCountBytesDelta;
        serverSentCountBytes += serverSentCountBytesDelta;
        clientSentCount += clientSentCountDelta;
        clientSentCountBytes += clientSentCountBytesDelta;
        clientReceivedCountBytes += clientReceivedCountBytesDelta;
        clientErrorCount += clientErrorsDelta;
        serverErrorCount += serverErrorsDelta;
    }

    static Matcher<String> assertClientMessagesCountBytesSent(String path, int clientSentCountBytes) {
        return assertTotal(CLIENT_MESSAGES_COUNT_SENT_BYTES, clientSentCountBytes, path);
    }

    static Matcher<String> assertClientMessagesCountBytesReceived(String path, int clientReceivedCountBytes) {
        return assertTotal(CLIENT_MESSAGES_COUNT_RECEIVED_BYTES, clientReceivedCountBytes, path);
    }

    static Matcher<String> assertClientMessagesCountSent(String path, int clientSentCount) {
        return assertTotal(CLIENT_MESSAGES_COUNT_SENT, clientSentCount, path);
    }

    static Matcher<String> assertServerMessagesCountReceived(String path, int serverReceivedCount) {
        return assertTotal(SERVER_MESSAGES_COUNT_RECEIVED, serverReceivedCount, path);
    }

    static Matcher<String> assertServerMessagesCountBytesSent(String path, int serverSentCountBytes) {
        return assertTotal(SERVER_MESSAGES_COUNT_SENT_BYTES, serverSentCountBytes, path);
    }

    static Matcher<String> assertServerMessagesCountBytesReceived(String path, int serverReceivedCountBytes) {
        return assertTotal(SERVER_MESSAGES_COUNT_RECEIVED_BYTES, serverReceivedCountBytes, path);
    }

    static Matcher<String> assertServerErrorTotal(String path, int serverErrorCount) {
        return assertTotal(SERVER_MESSAGES_COUNT_ERRORS, serverErrorCount, path);
    }

    static Matcher<String> assertClientErrorTotal(String path, int clientErrorCount) {
        return assertTotal(CLIENT_MESSAGES_COUNT_ERRORS, clientErrorCount, path);
    }

    static Matcher<String> assertServerConnectionOpeningFailedTotal(String path, int serverConnectionOpeningFailedCount) {
        return assertTotal(SERVER_CONNECTION_OPENED_ERROR, serverConnectionOpeningFailedCount, path);
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
        return assertTotal(SERVER_CONNECTION_OPENED, serverConnectionOpenedCount, path);
    }

    static Matcher<String> assertClientConnectionOpenedTotal(String path, int clientConnectionOpenedCount) {
        return assertTotal(CLIENT_CONNECTION_OPENED, clientConnectionOpenedCount, path);
    }

    static Matcher<String> assertServerConnectionClosedTotal(String path, int serverConnectionClosedCount) {
        return assertTotal(SERVER_CONNECTION_CLOSED, serverConnectionClosedCount, path);
    }

    static Matcher<String> assertClientConnectionClosedTotal(String path, int clientConnectionClosedCount) {
        return assertTotal(CLIENT_CONNECTION_CLOSED, clientConnectionClosedCount, path);
    }

    private static Matcher<String> assertTotal(String metricKey, int expectedCount, String path) {
        var prometheusFormatKey = "%s_total".formatted(toPrometheusFormat(metricKey));
        return new BaseMatcher<>() {
            @Override
            public boolean matches(Object o) {
                if (o instanceof String str) {
                    var sameKeyMultipleTags = str
                            .lines()
                            .filter(l -> l.contains(prometheusFormatKey))
                            .filter(l -> path == null || l.contains(path)) // filter by path
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
                description.appendText("Key '%s' with value '%d'".formatted(prometheusFormatKey, expectedCount));
            }
        };
    }

    private static String toPrometheusFormat(String dottedMicrometerFormat) {
        return dottedMicrometerFormat.replace(".", "_").replace("-", "_");
    }

    private static ValidatableResponse getMetrics() {
        return RestAssured.given().get("/q/metrics").then().statusCode(200);
    }

    static void assertMetrics(Consumer<ValidatableResponse> assertion) {
        Awaitility.await().atMost(Duration.ofSeconds(12)).untilAsserted(() -> assertion.accept(getMetrics()));
    }

    static int stringToBytes(String... messages) {
        return Arrays.stream(messages).map(msg -> msg.getBytes(StandardCharsets.UTF_8)).map(s -> s.length).reduce(0,
                Integer::sum);
    }
}
