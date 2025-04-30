package io.quarkus.websockets.next.test.telemetry;

import static io.quarkus.websockets.next.test.telemetry.AbstractWebSocketsOnMessageTest.createQuarkusUnitTest;
import static io.quarkus.websockets.next.test.telemetry.AbstractWebSocketsOnMessageTest.getMetrics;
import static io.quarkus.websockets.next.test.telemetry.ExpectedServerEndpointResponse.ECHO_RESPONSE;
import static io.quarkus.websockets.next.test.telemetry.ExpectedServerEndpointResponse.NO_RESPONSE;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerConnectionOpenedTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerConnectionOpeningFailedTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.stringToBytes;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.WebSocketConnector;
import io.quarkus.websockets.next.test.telemetry.endpoints.onerror.ErroneousClient_NoOnError;
import io.quarkus.websockets.next.test.telemetry.endpoints.onerror.ErroneousClient_OverloadedOnError;
import io.quarkus.websockets.next.test.telemetry.endpoints.onerror.ErroneousServerEndpoint_OnClose;
import io.quarkus.websockets.next.test.telemetry.endpoints.onerror.ErroneousServerEndpoint_OverriddenOnError;
import io.quarkus.websockets.next.test.telemetry.endpoints.onerror.GlobalErrorHandler;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;

public class MicrometerWebSocketsOnErrorTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = createQuarkusUnitTest(
            "io.quarkus.websockets.next.test.telemetry.endpoints.onerror");

    @Inject
    WebSocketConnector<ErroneousClient_NoOnError> erroneousClientConnector_NoOnErr;

    @Inject
    WebSocketConnector<ErroneousClient_OverloadedOnError> erroneousClientConnector_OverloadedOnErr;

    @TestHTTPResource("/")
    URI baseUri;

    @TestHTTPResource("server-error-no-on-error")
    URI serverEndpoint_NoExplicitOnError_Uri;

    @TestHTTPResource("server-error-overridden-on-error")
    URI serverEndpoint_OverriddenOnError_Uri;

    @TestHTTPResource("server-error-on-open")
    URI serverEndpoint_ErrorOnOpen_Uri;

    @TestHTTPResource("server-error-on-close")
    URI serverEndpoint_ErrorOnClose_Uri;

    @TestHTTPResource("server-error-global-handler")
    URI serverEndpoint_GlobalErrorHandler_Uri;

    @Inject
    Vertx vertx;

    private final MetricsAsserter asserter = new MetricsAsserter();

    @Test
    public void testClientEndpointError_ExceptionInsideOnTextMessage_noExplicitOnErrorDefined() {
        // client endpoint doesn't define @OnError
        // @OnTextMessage results in a failure

        var clientConn = erroneousClientConnector_NoOnErr.baseUri(baseUri).connectAndAwait();
        asserter.serverConnectionOpenedCount += 1;
        asserter.clientConnectionOpenedCount += 1;
        var msg = "What'd I Say";
        // 1 sent: use 'clientConn' to send 'msg'
        // 1 received, 2 sent: 'ErroneousClientEndpoint_NoOnError' -> 'Multi<String> onMessage(String message)', 2 in Multi
        // 2 received: 'ErroneousClient_NoOnError' -> 'Uni<Void> onMessage(String message)'
        int clientBytesSent = stringToBytes(msg);
        int clientBytesReceived = stringToBytes("echo 0: " + msg, "echo 1: " + msg);
        int serverBytesReceived = clientBytesSent;
        int serverBytesSent = clientBytesReceived;

        clientConn.sendTextAndAwait(msg);
        Awaitility.await().untilAsserted(() -> Assertions.assertEquals(2, ErroneousClient_NoOnError.MESSAGES.size()));
        asserter.assertTotalMetricsForAllPaths(0, 0, 1, serverBytesReceived, serverBytesSent, 1, clientBytesSent,
                clientBytesReceived, 2, 2);

        // 'ErroneousClient_NoOnError' throws exception inside 'onMessage' after it received 4 messages
        clientConn.sendTextAndAwait(msg);
        Awaitility.await().untilAsserted(() -> Assertions.assertEquals(4, ErroneousClient_NoOnError.MESSAGES.size()));
        asserter.assertTotalMetricsForAllPaths(0, 1, 1, serverBytesReceived, serverBytesSent, 1, clientBytesSent,
                clientBytesReceived, 2, 2);

        clientConn.closeAndAwait();
    }

    @Test
    public void testClientEndpointError_ExceptionInsideOnTextMessage_WithOverloadedOnError() throws InterruptedException {
        // client endpoint defines multiple @OnError
        // @OnTextMessage results in a failure

        var clientConn = erroneousClientConnector_OverloadedOnErr.baseUri(baseUri).connectAndAwait();
        asserter.serverConnectionOpenedCount += 1;
        asserter.clientConnectionOpenedCount += 1;
        var msg = "What'd I Say";
        int clientBytesSent = stringToBytes(msg);
        int clientBytesReceived = stringToBytes("echo 0: " + msg, "echo 1: " + msg);
        int serverBytesReceived = clientBytesSent;
        int serverBytesSent = clientBytesReceived;

        // 1 sent: use 'clientConn' to send 'msg'
        // 1 received, 2 sent: 'ErroneousClientEndpoint_OverloadedOnError' -> 'Multi<String> onMessage(String message)'
        // 2 received: 'ErroneousClient_OverloadedOnError' -> 'Uni<Void> onMessage(String message)'
        clientConn.sendTextAndAwait(msg);

        // assert messages and metrics
        Awaitility.await().untilAsserted(() -> Assertions.assertEquals(2, ErroneousClient_OverloadedOnError.MESSAGES.size()));
        // assert no client exception collected as metric
        asserter.assertTotalMetricsForAllPaths(0, 0, 1, serverBytesReceived, serverBytesSent, 1, clientBytesSent,
                clientBytesReceived, 2, 2);

        // 1 sent: use 'clientConn' to send 'msg'
        // 1 received, 2 sent: 'ErroneousClientEndpoint_OverloadedOnError' -> 'Multi<String> onMessage(String message)'
        // 2 received: 'ErroneousClient_OverloadedOnError' -> 'Uni<Void> onMessage(String message)'
        // after 4 messages, a RuntimeException is thrown
        // 1 sent: 'ErroneousClient_OverloadedOnError' recovers with 'recoveryMsg' in
        // @OnError 'String onError(RuntimeException e)'
        // 1 received, 2 sent: 'ErroneousClientEndpoint_OverloadedOnError' (that 'recoveryMsg')
        // 2 received: in 'ErroneousClient_OverloadedOnError'
        // === total expected: 6 received, 6 sent
        clientConn.sendTextAndAwait(msg);

        // client @OnError returns this String which is sent to the server @OnMessage, so expect extra bytes
        var recoveryMsg = "Expected error - 4 items";
        int extraClientSentBytes = stringToBytes(recoveryMsg);
        int extraServerReceivedBytes = extraClientSentBytes;
        int extraServerSentBytes = stringToBytes("echo 0: " + recoveryMsg, "echo 1: " + recoveryMsg);
        int extraClientReceivedBytes = extraServerSentBytes;

        // assert messages and metrics
        Awaitility.await().untilAsserted(() -> Assertions.assertEquals(6, ErroneousClient_OverloadedOnError.MESSAGES.size()));
        assertTrue(ErroneousClient_OverloadedOnError.RUNTIME_EXCEPTION_LATCH.await(2, TimeUnit.SECONDS));
        asserter.assertTotalMetricsForAllPaths(0, 1, 2, serverBytesReceived + extraServerReceivedBytes,
                serverBytesSent + extraServerSentBytes,
                2, clientBytesSent + extraClientSentBytes, clientBytesReceived + extraClientReceivedBytes, 4, 4);

        // 1 sent: use 'clientConn' to send 'msg'
        // 1 received, 2 sent: 'ErroneousClientEndpoint_OverloadedOnError' -> 'Multi<String> onMessage(String message)'
        // 2 received: 'ErroneousClient_OverloadedOnError' -> 'Uni<Void> onMessage(String message)'
        clientConn.sendTextAndAwait(msg);

        // assert messages and metrics
        Awaitility.await().untilAsserted(() -> Assertions.assertEquals(8, ErroneousClient_OverloadedOnError.MESSAGES.size()));
        // after 8 messages, an IllegalStateException is thrown
        // @OnError void onError(IllegalStateException e)
        assertTrue(ErroneousClient_OverloadedOnError.ILLEGAL_STATE_EXCEPTION_LATCH.await(2, TimeUnit.SECONDS));
        asserter.assertTotalMetricsForAllPaths(0, 1, 1, serverBytesReceived, serverBytesSent, 1, clientBytesSent,
                clientBytesReceived, 2, 2);

        clientConn.closeAndAwait();
    }

    @Test
    public void testServerEndpointError_ExceptionDuringTextDecoding_noExplicitOnErrorDefined() {
        // server endpoint @OnTextMessage: Uni<Dto> onMessage(Multi<Dto> dto)
        // text codec throws exception
        // no explicit @OnError
        var connection = Connection.of(serverEndpoint_NoExplicitOnError_Uri, false, new String[] { "Billions" },
                NO_RESPONSE);
        asserter.serverConnectionOpenedCount += 1;
        connection.openConnectionThenSend(vertx);
        int serverSentCountBytesDelta = stringToBytes(connection.expectedResponses());
        int serverReceivedCountBytesDelta = stringToBytes(connection.messagesToSend());
        asserter.assertTotalMetricsForAllPaths(1, 0, 1, serverReceivedCountBytesDelta,
                serverSentCountBytesDelta, 0, 0, 0, 0, 0);
    }

    @Test
    public void testServerEndpointError_ExceptionDuringBinaryDecoding_OnErrorOverloaded() throws InterruptedException {
        // server endpoint @OnBinaryMessage: Uni<Dto> onMessage(Multi<Dto> dto)
        // @OnError: void onError(RuntimeException e)
        var msg = "Wendy";
        var connection = Connection.of(serverEndpoint_OverriddenOnError_Uri, true, new String[] { msg }, NO_RESPONSE);
        asserter.serverConnectionOpenedCount += 1;
        connection.openConnectionThenSend(vertx);
        assertTrue(ErroneousServerEndpoint_OverriddenOnError.RUNTIME_EXCEPTION_LATCH.await(5, TimeUnit.SECONDS));
        int serverSentCountBytesDelta = stringToBytes(connection.expectedResponses());
        int serverReceivedCountBytesDelta = stringToBytes(connection.messagesToSend());
        asserter.assertTotalMetricsForAllPaths(1, 0, 1, serverReceivedCountBytesDelta,
                serverSentCountBytesDelta, 0, 0, 0, 0, 0);
    }

    @Test
    public void testServerEndpointError_ExceptionInsideOnOpen() {
        // error happens in @OnOpen, @OnTextMessage is invoked but the connection is already closed
        var connection = Connection.of(serverEndpoint_ErrorOnOpen_Uri, false, new String[] { "Rhodes" }, NO_RESPONSE);
        asserter.serverConnectionOpenedCount += 1;
        connection.openConnectionThenSend(vertx);
        int serverSentCountBytesDelta = stringToBytes(connection.expectedResponses());
        int serverReceivedCountBytesDelta = stringToBytes(connection.messagesToSend());
        asserter.assertTotalMetricsForAllPaths(1, 0, 1, serverReceivedCountBytesDelta,
                serverSentCountBytesDelta, 0, 0, 0, 0, 0);
    }

    @Test
    public void testServerEndpointError_ExceptionInsideOnClose() throws InterruptedException {
        // @OnBinaryMessage is called: Multi<String> onMessage(String message)
        // expect 1 received message and one response
        // @OnClose fails with IllegalStateException
        // explicitly declared @OnError catches the exception
        var connection = Connection.of(serverEndpoint_ErrorOnClose_Uri, "Bobby", true, "Chuck");
        asserter.serverConnectionOpenedCount += 1;
        connection.openConnectionThenSend(vertx);
        assertTrue(ErroneousServerEndpoint_OnClose.ILLEGAL_STATE_EXCEPTION_LATCH.await(7, TimeUnit.SECONDS));
        int serverSentCountBytesDelta = stringToBytes(connection.expectedResponses());
        int serverReceivedCountBytesDelta = stringToBytes(connection.messagesToSend());
        asserter.assertTotalMetricsForAllPaths(1, 0, 1, serverReceivedCountBytesDelta,
                serverSentCountBytesDelta, 0, 0, 0, 1, 0);
    }

    @Test
    public void testServerEndpointError_GlobalErrorHandler() throws InterruptedException {
        // test that error handled by a global error handler (defined outside the endpoint) are accounted for
        // global error handler recovers exception with original message: String onError(IllegalArgumentException e)
        // we need to check that both error and response sent from the global handler (bytes) are collected as a metric
        var sentMessages = new String[] { "Hold the Line" };
        var connection = Connection.of(serverEndpoint_GlobalErrorHandler_Uri, false, sentMessages,
                ECHO_RESPONSE.getExpectedResponse(sentMessages));
        asserter.serverConnectionOpenedCount += 1;
        connection.openConnectionThenSend(vertx);
        assertTrue(GlobalErrorHandler.ILLEGAL_ARGUMENT_EXCEPTION_LATCH.await(5, TimeUnit.SECONDS));
        // on each message, an exception is raised, we send 1 message -> expect 1 error
        int serverSentCountBytesDelta = stringToBytes(connection.expectedResponses());
        int serverReceivedCountBytesDelta = stringToBytes(connection.messagesToSend());
        asserter.assertTotalMetricsForAllPaths(1, 0, 1, serverReceivedCountBytesDelta,
                serverSentCountBytesDelta, 0, 0, 0, 1, 0);
    }

    @Test
    public void testServerEndpoint_HttpUpgradeFailed() {
        // opening WebSocket connection failed and we need it tracked
        var path = "/http-upgrade-failed";
        asserter.serverConnectionOpenedCount += 1;
        RestAssured.given()
                // without this header the client would receive 404
                .header("Sec-WebSocket-Key", "foo")
                .get(path).then().statusCode(400);
        Awaitility.await().atMost(Duration.ofSeconds(12)).untilAsserted(() -> {
            getMetrics()
                    .body(assertServerConnectionOpenedTotal(asserter.serverConnectionOpenedCount))
                    .body(assertServerConnectionOpenedTotal(path, 1))
                    .body(assertServerConnectionOpeningFailedTotal(path, 1));
        });
    }
}
