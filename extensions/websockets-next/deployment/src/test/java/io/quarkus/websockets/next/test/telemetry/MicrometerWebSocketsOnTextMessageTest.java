package io.quarkus.websockets.next.test.telemetry;

import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertClientConnectionClosedTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertClientConnectionOpenedTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertClientMessagesCountBytesReceived;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertClientMessagesCountBytesSent;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertClientMessagesCountSent;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerConnectionClosedTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerConnectionOpenedTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerMessagesCountBytesReceived;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerMessagesCountBytesSent;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerMessagesCountReceived;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.stringToBytes;

import java.net.URI;
import java.time.Duration;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.WebSocketConnector;
import io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage.BounceClient;
import io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage.ClientEndpointWithPathParams;
import io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage.MultiClient;
import io.vertx.core.buffer.Buffer;

/**
 * Tests metrics for {@link io.quarkus.websockets.next.OnTextMessage}.
 */
public class MicrometerWebSocketsOnTextMessageTest extends AbstractWebSocketsOnMessageTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = createQuarkusUnitTest(
            "io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage");

    @Inject
    WebSocketConnector<BounceClient> bounceClientConnector;

    @Inject
    WebSocketConnector<MultiClient> multiClientConnector;

    @Inject
    WebSocketConnector<ClientEndpointWithPathParams> clientWithPathParamsConnector;

    @TestHTTPResource("/ping/ho/and/hey")
    URI testServerPathParam1;

    @TestHTTPResource("/ping/amy/and/macdonald")
    URI testServerPathParam2;

    @Override
    protected boolean binaryMode() {
        return false;
    }

    @Override
    protected WebSocketConnector<?> bounceClientConnector() {
        return bounceClientConnector;
    }

    @Override
    protected WebSocketConnector<?> multiClientConnector() {
        return multiClientConnector;
    }

    @Test
    public void testServerEndpoint_PathParams_ResponseFromOnOpenMethod() {
        // endpoint: @OnOpen String process(@PathParam String one, @PathParam String two)
        // path: /ping/{one}/and/{two} -> one:two
        var path = "/ping/:one/and/:two";
        var expectedResponse = "ho:hey"; // path is /ping/ho/and/hey
        var connection1 = Connection.of(testServerPathParam1, expectedResponse, binaryMode(), "whatever");
        asserter.serverConnectionOpenedCount += 1;
        connection1.openConnectionThenSend(vertx);

        // assert totals for all the path tags
        int serverSentCountBytesDelta = stringToBytes(connection1.expectedResponses());
        int serverReceivedCountBytesDelta = stringToBytes(connection1.messagesToSend());
        asserter.assertTotalMetricsForAllPaths(0, 0, 1, serverReceivedCountBytesDelta,
                serverSentCountBytesDelta, 0, 0, 0, 1, 0);
        // assert metric for our path tag only (this is sent from @OnOpen)
        int serverBytesSent1 = stringToBytes(expectedResponse);
        Awaitility.await().atMost(Duration.ofSeconds(12)).untilAsserted(() -> getMetrics()
                .body(assertServerMessagesCountBytesSent(path, serverBytesSent1)));

        var expectedResponse2 = "amy:macdonald"; // path is /ping/amy/and/macdonald
        var connection2 = Connection.of(testServerPathParam2, expectedResponse2, binaryMode(), "whatever");
        asserter.serverConnectionOpenedCount += 1;
        connection2.openConnectionThenSend(vertx);

        // assert totals for all the path tags
        serverSentCountBytesDelta = stringToBytes(connection2.expectedResponses());
        serverReceivedCountBytesDelta = stringToBytes(connection2.messagesToSend());
        asserter.assertTotalMetricsForAllPaths(0, 0, 1, serverReceivedCountBytesDelta,
                serverSentCountBytesDelta, 0, 0, 0, 1, 0);
        // assert metric for our path tag only (this is sent from @OnOpen) (no deltas, so previous bytes + current ones)
        int serverBytesSent2 = stringToBytes(expectedResponse2);
        Awaitility.await().atMost(Duration.ofSeconds(12)).untilAsserted(() -> getMetrics()
                .body(assertServerMessagesCountBytesSent(path, serverBytesSent1 + serverBytesSent2)));
    }

    @Test
    public void testClientEndpoint_PathParam() {
        // server endpoint: Uni<String> onMessage(String message)
        // client endpoint: void onTextMessage(String message)
        var msg = "Ut enim ad minim veniam";

        var clientConn = clientWithPathParamsConnector
                .baseUri(baseUri)
                .pathParam("name", "Lu=")
                .connectAndAwait();
        asserter.serverConnectionOpenedCount += 1;
        asserter.clientConnectionOpenedCount += 1;
        if (binaryMode()) {
            clientConn.sendBinaryAndAwait(Buffer.buffer(msg));
        } else {
            clientConn.sendTextAndAwait(msg);
        }
        // 'clientConn' sends 'Ut enim ad minim veniam'
        // server endpoint - 1 received, 1 sent: Uni<String> onMessage(String message)
        // client endpoint - 1 received: void onTextMessage(String message)
        // that is received 2 messages and sent 2 messages
        int clientBytesReceived = stringToBytes("echo 0: " + msg);
        int clientBytesSent = stringToBytes(msg);
        int serverBytesReceived = clientBytesSent;
        int serverBytesSent = clientBytesReceived;

        // assert totals for all the path tags
        asserter.assertTotalMetricsForAllPaths(0, 0, 1, serverBytesReceived, serverBytesSent, 1, clientBytesSent,
                clientBytesReceived, 1, 1);
        // assert metric for our path tag only
        clientConn.closeAndAwait();
        Awaitility.await().atMost(Duration.ofSeconds(12)).untilAsserted(() -> getMetrics()
                .body(assertClientConnectionClosedTotal("/client-endpoint-with-path-param/{name}", 1))
                .body(assertClientConnectionOpenedTotal("/client-endpoint-with-path-param/{name}", 1))
                .body(assertClientMessagesCountBytesSent("/client-endpoint-with-path-param/{name}", clientBytesSent))
                .body(assertClientMessagesCountSent("/client-endpoint-with-path-param/{name}", 1))
                .body(assertClientMessagesCountBytesReceived("/client-endpoint-with-path-param/{name}", clientBytesReceived))
                .body(assertServerConnectionClosedTotal("/client-endpoint-with-path-param/:name", 1))
                .body(assertServerMessagesCountReceived("/client-endpoint-with-path-param/:name", 1))
                .body(assertServerConnectionOpenedTotal("/client-endpoint-with-path-param/:name", 1))
                .body(assertServerMessagesCountBytesReceived("/client-endpoint-with-path-param/:name", serverBytesReceived))
                .body(assertServerMessagesCountBytesSent("/client-endpoint-with-path-param/:name", serverBytesSent)));

        clientConn = clientWithPathParamsConnector
                .baseUri(baseUri)
                .pathParam("name", "Go=Through")
                .connectAndAwait();
        asserter.serverConnectionOpenedCount += 1;
        asserter.clientConnectionOpenedCount += 1;
        if (binaryMode()) {
            clientConn.sendBinaryAndAwait(Buffer.buffer(msg));
        } else {
            clientConn.sendTextAndAwait(msg);
        }

        // assert totals for all the path tags
        asserter.assertTotalMetricsForAllPaths(0, 0, 1, serverBytesReceived, serverBytesSent, 1, clientBytesSent,
                clientBytesReceived, 1, 1);
        // assert metric for our path tag only (prev + current ones, no deltas here)
        clientConn.closeAndAwait();
        Awaitility.await().atMost(Duration.ofSeconds(12)).untilAsserted(() -> {
            getMetrics()
                    .body(assertClientConnectionOpenedTotal("/client-endpoint-with-path-param/{name}", 2))
                    .body(assertClientConnectionClosedTotal("/client-endpoint-with-path-param/{name}", 2))
                    .body(assertClientMessagesCountBytesSent("/client-endpoint-with-path-param/{name}", clientBytesSent * 2))
                    .body(assertClientMessagesCountSent("/client-endpoint-with-path-param/{name}", 2))
                    .body(assertClientMessagesCountBytesReceived("/client-endpoint-with-path-param/{name}",
                            clientBytesReceived * 2))
                    .body(assertServerConnectionOpenedTotal("/client-endpoint-with-path-param/:name", 2))
                    .body(assertServerConnectionClosedTotal("/client-endpoint-with-path-param/:name", 2))
                    .body(assertServerMessagesCountReceived("/client-endpoint-with-path-param/:name", 2))
                    .body(assertServerMessagesCountBytesReceived("/client-endpoint-with-path-param/:name",
                            serverBytesReceived * 2))
                    .body(assertServerMessagesCountBytesSent("/client-endpoint-with-path-param/:name", serverBytesSent * 2));
        });
    }

}
