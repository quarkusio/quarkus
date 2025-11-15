package io.quarkus.websockets.next.test.telemetry;

import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerConnectionClosedTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerConnectionOpenedTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerConnectionOpeningFailedTotal;
import static io.quarkus.websockets.next.test.telemetry.MetricsAsserter.assertServerErrorTotal;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.logging.Log;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

public class MicrometerWebSocketsStandaloneTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(MetricsAsserter.class)
                    .addAsResource(new StringAsset("""
                            bounce-endpoint.prefix-responses=true
                            quarkus.websockets-next.server.metrics.enabled=true
                            quarkus.websockets-next.client.metrics.enabled=true
                            """), "application.properties"))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-micrometer-registry-prometheus-deployment",
                            Version.getVersion())));

    @Inject
    WebSocketConnector<ErroneousClient_OnConnectError> onConnectErrorClient;

    @Inject
    WebSocketConnector<ErroneousClient_OnConnectErrorHandler> onConnectErrorHandlerClient;

    @TestHTTPResource("/")
    URI baseUri;

    static ValidatableResponse getMetrics() {
        return RestAssured.given().get("/q/metrics").then().statusCode(200);
    }

    @Test
    public void testServerEndpoint_OnConnectionError() {
        WebSocketClientConnection connection = onConnectErrorClient.baseUri(baseUri).connectAndAwait();
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            // Connection closing is async and takes some time
            assertFalse(connection.isOpen(),
                    "Runtime exception happened on server side and connection is still open");
        });

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            getMetrics()
                    .body(assertServerConnectionOpeningFailedTotal("/on-connect-error", 1))
                    .body(assertServerErrorTotal("/on-connect-error", 1))
                    .body(assertServerConnectionOpenedTotal("/on-connect-error", 1))
                    .body(assertServerConnectionClosedTotal("/on-connect-error", 1));
        });
    }

    @Test
    public void testServerEndpoint_OnConnectionErrorHandler() {
        WebSocketClientConnection connection = onConnectErrorHandlerClient.baseUri(baseUri).connectAndAwait();
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            // Connection closing will never close because there is an OnError handler.
            assertTrue(connection.isOpen(), "Should never close");
        });

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            getMetrics()
                    .body(assertServerConnectionOpeningFailedTotal("/with-on-connect-error-handler", 0))
                    .body(assertServerErrorTotal("/with-on-connect-error-handler", 1))
                    .body(assertServerConnectionOpenedTotal("/with-on-connect-error-handler", 1))
                    .body(assertServerConnectionClosedTotal("/with-on-connect-error-handler", 0));

        });
    }

    // -----------------------  SERVERS -------------------------------

    @WebSocket(path = "/on-connect-error")
    public static class OnConnectErrorEndpoint {

        @OnOpen()
        public void onOpen() {
            Log.info("onOpen throwing exception");
            throw new RuntimeException("Crafted exception - Websocket failed to open");
        }
    }

    @WebSocket(path = "/with-on-connect-error-handler")
    public static class OnConnectErrorHandlerEndpoint {

        @OnOpen()
        public void onOpen() {
            Log.info("onOpen throwing exception");
            throw new RuntimeException("Crafted exception - Websocket failed to open");
        }

        @OnError
        public void onError(Exception failure) {
            Log.warnv("Error handled: {0}", failure.getMessage());
        }
    }

    // ------------------------- CLIENTS -------------------------------

    @WebSocketClient(path = "/on-connect-error")
    public static class ErroneousClient_OnConnectError {

        @OnOpen()
        public void onOpen() {
            Log.info("client onOpen");
        }
    }

    @WebSocketClient(path = "/with-on-connect-error-handler")
    public static class ErroneousClient_OnConnectErrorHandler {

        @OnOpen()
        public void onOpen() {
            Log.info("client onOpen");
        }
    }
}
