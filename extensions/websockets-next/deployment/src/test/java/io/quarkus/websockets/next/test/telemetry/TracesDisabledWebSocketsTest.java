package io.quarkus.websockets.next.test.telemetry;

import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.WebSocketConnector;
import io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage.BounceClient;
import io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage.BounceEndpoint;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketConnectOptions;

public class TracesDisabledWebSocketsTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(BounceEndpoint.class, WSClient.class, InMemorySpanExporterProducer.class, BounceClient.class)
                    .addAsResource(new StringAsset("""
                            quarkus.otel.bsp.export.timeout=1s
                            quarkus.otel.bsp.schedule.delay=50
                            quarkus.websockets-next.server.traces.enabled=false
                            quarkus.websockets-next.client.traces.enabled=false
                            """), "application.properties"))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-opentelemetry-deployment", Version.getVersion())));

    @TestHTTPResource("bounce")
    URI bounceUri;

    @TestHTTPResource
    URI baseUri;

    @Inject
    Vertx vertx;

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    WebSocketConnector<BounceClient> connector;

    @BeforeEach
    public void resetSpans() {
        spanExporter.reset();
        BounceEndpoint.connectionId = null;
        BounceEndpoint.endpointId = null;
        BounceEndpoint.MESSAGES.clear();
        BounceClient.MESSAGES.clear();
        BounceClient.CLOSED_LATCH = new CountDownLatch(1);
        BounceEndpoint.CLOSED_LATCH = new CountDownLatch(1);
    }

    @Test
    public void testServerEndpointTracesDisabled() {
        assertEquals(0, spanExporter.getFinishedSpanItems().size());
        try (WSClient client = new WSClient(vertx)) {
            client.connect(new WebSocketConnectOptions(), bounceUri);
            var response = client.sendAndAwaitReply("How U Livin'").toString();
            assertEquals("How U Livin'", response);
        }
        waitForInitialRequestTrace();

        // check HTTP server traces still enabled
        var initialRequestSpan = getInitialRequestSpan();
        assertEquals(bounceUri.getPath(), initialRequestSpan.getAttributes().get(URL_PATH));

        // check WebSocket server endpoint traces are disabled
        assertEquals(1, spanExporter.getFinishedSpanItems().size());
    }

    @Test
    public void testClientAndServerEndpointTracesDisabled() throws InterruptedException {
        var clientConn = connector.baseUri(baseUri).connectAndAwait();
        clientConn.sendTextAndAwait("Make It Bun Dem");

        // assert client and server called
        Awaitility.await().untilAsserted(() -> {
            assertEquals(1, BounceEndpoint.MESSAGES.size());
            assertEquals("Make It Bun Dem", BounceEndpoint.MESSAGES.get(0));
            assertEquals(1, BounceClient.MESSAGES.size());
            assertEquals("Make It Bun Dem", BounceClient.MESSAGES.get(0));
        });

        clientConn.closeAndAwait();
        // assert connection closed and client/server were notified
        assertTrue(BounceClient.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(BounceEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));

        waitForInitialRequestTrace();

        // check HTTP server traces still enabled
        var initialRequestSpan = getInitialRequestSpan();
        assertEquals("", initialRequestSpan.getAttributes().get(URL_PATH));

        // check both client and server WebSocket endpoint traces are disabled
        assertEquals(1, spanExporter.getFinishedSpanItems().size());
    }

    private void waitForInitialRequestTrace() {
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertEquals(1, spanExporter.getFinishedSpanItems().size()));
    }

    private SpanData getInitialRequestSpan() {
        return spanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> "GET /bounce".equals(sd.getName()))
                .filter(sd -> sd.getKind() == SpanKind.SERVER)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected span name 'GET /bounce' and kind '" + SpanKind.SERVER + "' not found: "
                                + spanExporter.getFinishedSpanItems()));
    }
}
