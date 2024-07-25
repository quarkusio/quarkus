package io.quarkus.websockets.next.test.telemetry;

import static io.quarkus.websockets.next.test.telemetry.Connection.sendAndAssertResponses;
import static io.quarkus.websockets.next.test.telemetry.ExpectedServerEndpointResponse.DOUBLE_ECHO_RESPONSE;
import static io.quarkus.websockets.next.test.telemetry.ExpectedServerEndpointResponse.ECHO_RESPONSE;
import static io.quarkus.websockets.next.test.telemetry.ExpectedServerEndpointResponse.NO_RESPONSE;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertClientConnectionClosedTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertClientConnectionOpenedTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertClientErrorTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertClientMessagesCountBytesReceived;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertClientMessagesCountBytesSent;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertClientMessagesCountSent;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertMetrics;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerConnectionClosedTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerConnectionOpenedTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerErrorTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerMessagesCountBytesReceived;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerMessagesCountBytesSent;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerMessagesCountReceived;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.stringToBytes;
import static io.quarkus.websockets.next.test.utils.WSClient.ReceiverMode.BINARY;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import io.quarkus.websockets.next.test.telemetry.ExpectedServerEndpointResponse.DoubleEchoExpectedServerEndpointResponse;
import io.quarkus.websockets.next.test.telemetry.ExpectedServerEndpointResponse.EchoExpectedServerEndpointResponse;
import io.quarkus.websockets.next.test.telemetry.ExpectedServerEndpointResponse.NoExpectedServerEndpointResponse;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocketConnectOptions;

public abstract class AbstractWebSocketsOnMessageTest {

    static QuarkusUnitTest createQuarkusUnitTest(String endpointsPackage) {
        return new QuarkusUnitTest()
                .withApplicationRoot(root -> root
                        .addPackage(endpointsPackage)
                        .addClasses(WSClient.class, Connection.class, MetricsAsserter.class,
                                AbstractWebSocketsOnMessageTest.class, ExpectedServerEndpointResponse.class,
                                NoExpectedServerEndpointResponse.class, EchoExpectedServerEndpointResponse.class,
                                DoubleEchoExpectedServerEndpointResponse.class)
                        .addAsResource(new StringAsset("""
                                bounce-endpoint.prefix-responses=true
                                """), "application.properties"))
                .setForcedDependencies(
                        List.of(Dependency.of("io.quarkus", "quarkus-micrometer-registry-prometheus-deployment",
                                Version.getVersion())));
    }

    protected final MetricsAsserter asserter = new MetricsAsserter();

    @TestHTTPResource("bounce")
    URI bounceUri;

    @TestHTTPResource("/")
    URI baseUri;

    @TestHTTPResource("received-multi-text-response-none-2")
    URI multiTextReceived_NoResponse_Uri_2;

    @TestHTTPResource("broadcast")
    URI broadcast_Uri;

    @Inject
    Vertx vertx;

    protected abstract boolean binaryMode();

    protected abstract WebSocketConnector<?> bounceClientConnector();

    protected abstract WebSocketConnector<?> multiClientConnector();

    @ParameterizedTest
    @MethodSource("provideServerEndpointDescription")
    public void testServerEndpoint(String path, String[] messagesToSend, String[] expectedResponses) {
        final WSClient client = binaryMode() ? new WSClient(vertx, BINARY) : new WSClient(vertx);
        try (client) {
            client.connect(new WebSocketConnectOptions(), baseUri.resolve(path));
            for (String message : messagesToSend) {
                if (binaryMode()) {
                    client.sendAndAwait(Buffer.buffer(message));
                } else {
                    client.sendAndAwait(message);
                }
            }
            client.waitForMessages(expectedResponses.length);
            var actualResponses = client.getMessages().stream().map(Buffer::toString).collect(Collectors.toSet());
            for (String expectedResponse : expectedResponses) {
                assertTrue(actualResponses.contains(expectedResponse),
                        () -> "Expected response '%s' not found, was: %s".formatted(expectedResponse, actualResponses));
            }
        }

        int serverReceivedCountDelta = messagesToSend.length;
        int serverReceivedCountBytesDelta = stringToBytes(messagesToSend);
        int serverSentCountBytesDelta = stringToBytes(expectedResponses);

        asserter.serverReceivedCount += serverReceivedCountDelta;
        asserter.serverReceivedCountBytes += serverReceivedCountBytesDelta;
        asserter.serverSentCountBytes += serverSentCountBytesDelta;
        asserter.serverConnectionOpenedCount += 1;

        assertMetrics(metrics -> metrics
                // test metrics per all the paths (regardless of the URI tag)
                .body(assertServerConnectionOpenedTotal(asserter.serverConnectionOpenedCount))
                .body(assertClientConnectionOpenedTotal(asserter.clientConnectionOpenedCount))
                .body(assertServerErrorTotal(asserter.serverErrorCount))
                .body(assertClientErrorTotal(asserter.clientErrorCount))
                .body(assertClientMessagesCountBytesSent(asserter.clientSentCountBytes))
                .body(assertClientMessagesCountBytesReceived(asserter.clientReceivedCountBytes))
                .body(assertClientMessagesCountSent(asserter.clientSentCount))
                .body(assertServerMessagesCountBytesReceived(asserter.serverReceivedCountBytes))
                .body(assertServerMessagesCountBytesSent(asserter.serverSentCountBytes))
                .body(assertServerMessagesCountReceived(asserter.serverReceivedCount))
                // test metrics per path tag
                .body(assertServerConnectionOpenedTotal(path, 1))
                .body(assertServerConnectionClosedTotal(path, 1))
                .body(assertClientConnectionOpenedTotal(path, 0))
                .body(assertClientConnectionClosedTotal(path, 0))
                .body(assertServerErrorTotal(path, 0))
                .body(assertClientErrorTotal(path, 0))
                .body(assertClientMessagesCountBytesSent(path, 0))
                .body(assertClientMessagesCountBytesReceived(path, 0))
                .body(assertClientMessagesCountSent(path, 0))
                .body(assertServerMessagesCountBytesReceived(path, serverReceivedCountBytesDelta))
                .body(assertServerMessagesCountBytesSent(path, serverSentCountBytesDelta))
                .body(assertServerMessagesCountReceived(path, serverReceivedCountDelta)));
    }

    private static Stream<Arguments> provideServerEndpointDescription() {
        var streamBuilder = Stream.<Arguments> builder();

        // test #1: testServerEndpoint_SingleTextReceived_NoSent
        // endpoint: void onMessage(String message)
        String[] sentMessages = new String[] { "Ballad of a Prodigal Son" };
        streamBuilder.add(Arguments.arguments("received-single-text-response-none", sentMessages, NO_RESPONSE));

        // test #2: testServerEndpoint_SingleTextReceived_SingleTextSent
        // endpoint: String onMessage(String message)
        sentMessages = new String[] { "Can't Find My Way Home" };
        streamBuilder.add(Arguments.arguments("single-text-received-single-text-sent", sentMessages,
                ECHO_RESPONSE.getExpectedResponse(sentMessages)));

        // test #3: testServerEndpoint_SingleTextReceived_MultiTextSent
        // endpoint: Multi<String> onMessage(String message)
        sentMessages = new String[] { "Always take a banana to a party" };
        streamBuilder.add(Arguments.arguments("received-single-text-response-multi-text", sentMessages,
                DOUBLE_ECHO_RESPONSE.getExpectedResponse(sentMessages)));

        // test #4: testServerEndpoint_MultiTextReceived_NoSent
        // endpoint: endpoint: void onMessage(Multi<String> message)
        sentMessages = new String[] { "When I go", "don't cry for me", "In my Father's arms I'll be",
                "The wounds this world left on my soul" };
        streamBuilder.add(Arguments.arguments("received-multi-text-response-none", sentMessages, NO_RESPONSE));

        // test #5: testServerEndpoint_MultiTextReceived_SingleTextSent
        // endpoint: String onMessage(Multi<String> message)
        sentMessages = new String[] { "Msg1", "Msg2", "Msg3", "Msg4" };
        streamBuilder.add(Arguments.arguments("received-multi-text-response-single-text", sentMessages,
                new String[] { "Alpha Shallows" }));

        // test #6: testServerEndpoint_MultiTextReceived_MultiTextSent
        // endpoint: Multi<String> onMessage(Multi<String> message)
        sentMessages = new String[] { "Msg1", "Msg2", "Msg3" };
        streamBuilder.add(Arguments.arguments("received-multi-text-response-multi-text", sentMessages,
                DOUBLE_ECHO_RESPONSE.getExpectedResponse(sentMessages)));

        // test #7: testServerEndpoint_SingleTextReceived_UniTextSent
        // endpoint: Uni<String> onMessage(String message)
        sentMessages = new String[] { "Bernie Sanders" };
        streamBuilder.add(Arguments.arguments("received-single-text-response-uni-text", sentMessages,
                ECHO_RESPONSE.getExpectedResponse(sentMessages)));

        // test #8: testServerEndpoint_SingleDtoReceived_NoSent
        // endpoint: void onMessage(Dto dto)
        sentMessages = new String[] { "major disappointment speaking" };
        streamBuilder.add(Arguments.arguments("received-single-dto-response-none", sentMessages, NO_RESPONSE));

        // test #9: testServerEndpoint_SingleDtoReceived_SingleDtoSent
        // endpoint: Dto onMessage(Dto dto)
        sentMessages = new String[] { "abcd123456" };
        streamBuilder.add(Arguments.arguments("received-single-dto-response-single-dto", sentMessages,
                ECHO_RESPONSE.getExpectedResponse(sentMessages)));

        // test #10: testServerEndpoint_SingleDtoReceived_UniDtoSent
        // endpoint: Uni<Dto> onMessage(Dto dto)
        sentMessages = new String[] { "Shot heard round the world" };
        streamBuilder.add(Arguments.arguments("received-single-dto-response-uni-dto", sentMessages,
                ECHO_RESPONSE.getExpectedResponse(sentMessages)));

        // test #11: testServerEndpoint_SingleDtoReceived_MultiDtoSent
        // endpoint: Multi<Dto> onMessage(Dto dto)
        sentMessages = new String[] { "Bananas are good" };
        streamBuilder.add(Arguments.arguments("received-single-dto-response-multi-dto", sentMessages,
                DOUBLE_ECHO_RESPONSE.getExpectedResponse(sentMessages)));

        // test #12: testServerEndpoint_MultiDtoReceived_NoSent
        // endpoint: void onMessage(Multi<Dto> dto)
        sentMessages = new String[] { "Tell me how ya livin", "Soljie what ya got givin" };
        streamBuilder.add(Arguments.arguments("received-multi-dto-response-none", sentMessages, NO_RESPONSE));

        // test #13: testServerEndpoint_MultiDtoReceived_SingleDtoSent
        // endpoint: Dto onMessage(Multi<Dto> message)
        sentMessages = new String[] { "Lorem ipsum dolor sit amet", "consectetur adipiscing elit",
                "sed do eiusmod tempor incididunt" };
        streamBuilder.add(Arguments.arguments("received-multi-dto-response-single-dto", sentMessages,
                new String[] { "ut labore et dolore magna aliqua" }));

        // test #14: testServerEndpoint_MultiDtoReceived_MultiDtoSent
        // endpoint: Multi<Dto> onMessage(Multi<Dto> dto)
        sentMessages = new String[] { "Right", "Left" };
        streamBuilder.add(Arguments.arguments("received-multi-dto-response-multi-dto", sentMessages,
                DOUBLE_ECHO_RESPONSE.getExpectedResponse(sentMessages)));

        return streamBuilder.build();
    }

    @Test
    public void testClientEndpoint_SingleTextReceived_NoSent() {
        var clientConn = bounceClientConnector().baseUri(baseUri).connectAndAwait();
        asserter.serverConnectionOpenedCount += 1;
        asserter.clientConnectionOpenedCount += 1;

        var msg1 = "Ut enim ad minim veniam";
        sendClientMessageAndWait(clientConn, msg1);
        // 'clientConn' sends 'Ut enim ad minim veniam'
        // 'BounceEndpoint' -> 'String onMessage(String message)' sends 'Response 0: Ut enim ad minim veniam'
        // 'BounceClient' -> 'void echo(String message)' receives 'Response 0: Ut enim ad minim veniam'
        // that is received 2 messages and sent 2 messages
        int clientBytesReceived = stringToBytes("echo 0: " + msg1);
        int clientBytesSent = stringToBytes(msg1);
        int serverBytesReceived = clientBytesSent;
        int serverBytesSent = clientBytesReceived;
        asserter.assertMetrics(0, 0, 1, serverBytesReceived, serverBytesSent, 1, clientBytesSent, clientBytesReceived);

        msg1 = "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat";
        var msg2 = "Duis aute irure dolor in reprehenderit";
        sendClientMessageAndWait(clientConn, msg1);
        sendClientMessageAndWait(clientConn, msg2);

        clientBytesReceived = stringToBytes("echo 0: " + msg1, "echo 0: " + msg2);
        clientBytesSent = stringToBytes(msg1, msg2);
        serverBytesReceived = clientBytesSent;
        serverBytesSent = clientBytesReceived;
        asserter.assertMetrics(0, 0, 2, serverBytesReceived, serverBytesSent, 2, clientBytesSent, clientBytesReceived);

        clientConn.closeAndAwait();
    }

    @Test
    public void testClientEndpoint_MultiTextReceived_MultiTextSent() {
        var clientConn = multiClientConnector().baseUri(baseUri).connectAndAwait();
        asserter.serverConnectionOpenedCount += 1;
        asserter.clientConnectionOpenedCount += 1;

        var msg1 = "in voluptate velit esse cillum dolore eu fugiat nulla pariatur";
        var msg2 = "Excepteur sint occaecat cupidatat non proident";
        sendClientMessageAndWait(clientConn, msg1);
        sendClientMessageAndWait(clientConn, msg2);

        // 2 sent: 'clientConn' sends 2 messages
        // 2 sent, 2 received: 'MultiEndpoint' -> 'Multi<String> echo(Multi<String> messages)' -> accepts and receives message
        // 2 sent, 2 received: 'MultiClient' -> 'Multi<String> echo(Multi<String> messages)' -> accepts, receives, adds "echo 0: "
        // 2 received: 'MultiEndpoint' -> accepts and returns empty Multi
        int clientBytesReceived = stringToBytes(msg1, msg2);
        int clientBytesSent = stringToBytes(msg1, msg2, msg1 + "echo 0: ", msg2 + "echo 0: ");
        int serverBytesReceived = clientBytesSent;
        int serverBytesSent = clientBytesReceived;

        asserter.assertMetrics(0, 0, 4, serverBytesReceived, serverBytesSent, 4, clientBytesSent, clientBytesReceived);

        clientConn.closeAndAwait();
    }

    @Test
    public void testServerEndpoint_broadcasting() {
        // broadcast = true
        // endpoint: String onMessage(String message)

        var msg1 = "It's alright ma";
        // expected metrics:
        // endpoint receives msg1
        // 2 connections are opened so 2 responses are expected
        int sentBytes = stringToBytes("echo 0: " + msg1, "echo 0: " + msg1);
        int receivedBytes = stringToBytes(msg1);
        var sentMessages = new String[] { msg1 };
        var connection1 = Connection.of(broadcast_Uri, true, binaryMode(), sentMessages,
                ECHO_RESPONSE.getExpectedResponse(sentMessages));

        var msg2 = "I'm Only Bleeding";
        // expected metrics:
        // endpoint receives msg2
        // 2 connections are opened so 2 responses are expected
        asserter.serverConnectionOpenedCount += 2;
        sentBytes += stringToBytes("echo 0: " + msg2, "echo 0: " + msg2);
        receivedBytes += stringToBytes(msg2);
        sentMessages = new String[] { msg2 };
        var connection2 = Connection.of(broadcast_Uri, true, binaryMode(), sentMessages,
                ECHO_RESPONSE.getExpectedResponse(sentMessages));
        sendAndAssertResponses(vertx, connection1, connection2);
        asserter.assertMetrics(0, 2, sentBytes, receivedBytes);
    }

    @Test
    public void testServerEndpoint_SingleTextReceived_SingleTextSent_MultipleConnections() {
        // endpoint: String onMessage(String message)
        // testing multiple connections because we need to know that same counter endpoint counter is used by connections
        var msg = "Can't Find My Way Home";
        var sentMessages = new String[] { msg };
        var expectedResponses = ECHO_RESPONSE.getExpectedResponse(sentMessages);

        try (var client1 = new WSClient(vertx)) {
            client1.connect(new WebSocketConnectOptions(), bounceUri);
            var connection1 = Connection.of(bounceUri, false, binaryMode(), sentMessages, expectedResponses);
            sendClientMessageAndWait(client1, msg);
            asserter.serverConnectionOpenedCount += 1;
            asserter.assertMetrics(0, 1, connection1);

            var connection2 = Connection.of(bounceUri, false, binaryMode(), sentMessages, expectedResponses);
            sendAndAssertResponses(vertx, connection2);
            asserter.serverConnectionOpenedCount += 1;
            asserter.assertMetrics(0, 1, connection2);

            var connection3 = Connection.of(bounceUri, false, binaryMode(), sentMessages, expectedResponses);
            sendAndAssertResponses(vertx, connection3);
            asserter.serverConnectionOpenedCount += 1;
            asserter.assertMetrics(0, 1, connection3);

            // --- try different endpoint - start
            // endpoint: void onMessage(Multi<String> message)
            var sentMessages2 = new String[] { "I get up in the evening", "I ain't nothing but tired",
                    "I could use just a little help" };
            var connection = Connection.of(multiTextReceived_NoResponse_Uri_2, false, binaryMode(), sentMessages2, NO_RESPONSE);
            sendAndAssertResponses(vertx, connection);
            asserter.serverConnectionOpenedCount += 1;
            asserter.assertMetrics(0, 3, connection);
            // --- try different endpoint - end

            var connection4 = Connection.of(bounceUri, false, binaryMode(), sentMessages, expectedResponses);
            sendAndAssertResponses(vertx, connection4);
            asserter.serverConnectionOpenedCount += 1;
            asserter.assertMetrics(0, 1, connection4);

            // send again message via the first connection that is still open
            sendClientMessageAndWait(client1, msg);
            asserter.assertMetrics(0, 1, connection1);
        }
    }

    private void sendClientMessageAndWait(WSClient client, String msg) {
        if (binaryMode()) {
            client.sendAndAwait(Buffer.buffer(msg));
        } else {
            client.sendAndAwait(msg);
        }
    }

    protected void sendClientMessageAndWait(WebSocketClientConnection clientConn, String msg1) {
        if (binaryMode()) {
            clientConn.sendBinaryAndAwait(Buffer.buffer(msg1));
        } else {
            clientConn.sendTextAndAwait(msg1);
        }
    }
}
