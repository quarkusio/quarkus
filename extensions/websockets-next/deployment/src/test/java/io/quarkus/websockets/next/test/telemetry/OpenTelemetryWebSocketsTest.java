package io.quarkus.websockets.next.test.telemetry;

import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.opentelemetry.api.common.AttributeKey;
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

public class OpenTelemetryWebSocketsTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(BounceEndpoint.class, WSClient.class, InMemorySpanExporterProducer.class, BounceClient.class)
                    .addAsResource(new StringAsset("""
                            quarkus.otel.bsp.export.timeout=1s
                            quarkus.otel.bsp.schedule.delay=50
                            """), "application.properties"))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-opentelemetry-deployment", Version.getVersion())));

    @TestHTTPResource("bounce")
    URI bounceUri;

    @TestHTTPResource("/")
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
    public void testServerEndpointTracesOnly() {
        assertEquals(0, spanExporter.getFinishedSpanItems().size());
        try (WSClient client = new WSClient(vertx)) {
            client.connect(new WebSocketConnectOptions(), bounceUri);
            var response = client.sendAndAwaitReply("How U Livin'").toString();
            assertEquals("How U Livin'", response);
        }
        waitForTracesToArrive(3);
        var initialRequestSpan = getSpanByName("GET /bounce", SpanKind.SERVER);

        var connectionOpenedSpan = getSpanByName("OPEN " + bounceUri.getPath(), SpanKind.SERVER);
        assertEquals(bounceUri.getPath(), getUriAttrVal(connectionOpenedSpan));
        assertEquals(initialRequestSpan.getSpanId(), connectionOpenedSpan.getLinks().get(0).getSpanContext().getSpanId());

        var connectionClosedSpan = getSpanByName("CLOSE " + bounceUri.getPath(), SpanKind.SERVER);
        assertEquals(bounceUri.getPath(), getUriAttrVal(connectionClosedSpan));
        assertEquals(BounceEndpoint.connectionId, getConnectionIdAttrVal(connectionClosedSpan));
        assertEquals(BounceEndpoint.endpointId, getEndpointIdAttrVal(connectionClosedSpan));
        assertEquals(1, connectionClosedSpan.getLinks().size());
        assertEquals(connectionOpenedSpan.getSpanId(), connectionClosedSpan.getLinks().get(0).getSpanContext().getSpanId());
    }

    @Test
    public void testClientAndServerEndpointTraces() throws InterruptedException {
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

        waitForTracesToArrive(5);

        // server traces
        var initialRequestSpan = getSpanByName("GET /bounce", SpanKind.SERVER);
        var connectionOpenedSpan = getSpanByName("OPEN " + bounceUri.getPath(), SpanKind.SERVER);
        assertEquals(bounceUri.getPath(), getUriAttrVal(connectionOpenedSpan));
        assertEquals(initialRequestSpan.getSpanId(), connectionOpenedSpan.getLinks().get(0).getSpanContext().getSpanId());
        var connectionClosedSpan = getSpanByName("CLOSE " + bounceUri.getPath(), SpanKind.SERVER);
        assertEquals(bounceUri.getPath(), getUriAttrVal(connectionClosedSpan));
        assertEquals(BounceEndpoint.connectionId, getConnectionIdAttrVal(connectionClosedSpan));
        assertEquals(BounceEndpoint.endpointId, getEndpointIdAttrVal(connectionClosedSpan));
        assertEquals(1, connectionClosedSpan.getLinks().size());
        assertEquals(connectionOpenedSpan.getSpanId(), connectionClosedSpan.getLinks().get(0).getSpanContext().getSpanId());

        // client traces
        connectionOpenedSpan = getSpanByName("OPEN " + bounceUri.getPath(), SpanKind.CLIENT);
        assertEquals(bounceUri.getPath(), getUriAttrVal(connectionOpenedSpan));
        assertTrue(connectionOpenedSpan.getLinks().isEmpty());
        connectionClosedSpan = getSpanByName("CLOSE " + bounceUri.getPath(), SpanKind.CLIENT);
        assertEquals(bounceUri.getPath(), getUriAttrVal(connectionClosedSpan));
        assertNotNull(getConnectionIdAttrVal(connectionClosedSpan));
        assertNotNull(getClientIdAttrVal(connectionClosedSpan));
        assertEquals(1, connectionClosedSpan.getLinks().size());
        assertEquals(connectionOpenedSpan.getSpanId(), connectionClosedSpan.getLinks().get(0).getSpanContext().getSpanId());
    }

    @Test
    public void testServerTracesWhenErrorOnMessage() {
        assertEquals(0, spanExporter.getFinishedSpanItems().size());
        try (WSClient client = new WSClient(vertx)) {
            client.connect(new WebSocketConnectOptions(), bounceUri);
            var response = client.sendAndAwaitReply("It's Alright, Ma").toString();
            assertEquals("It's Alright, Ma", response);
            response = client.sendAndAwaitReply("I'm Only Bleeding").toString();
            assertEquals("I'm Only Bleeding", response);

            client.sendAndAwait("throw-exception");
            Awaitility.await().atMost(Duration.ofSeconds(5)).until(client::isClosed);
            assertEquals(WebSocketCloseStatus.INTERNAL_SERVER_ERROR.code(), client.closeStatusCode());
        }
        waitForTracesToArrive(3);

        // server traces
        var initialRequestSpan = getSpanByName("GET /bounce", SpanKind.SERVER);
        var connectionOpenedSpan = getSpanByName("OPEN " + bounceUri.getPath(), SpanKind.SERVER);
        assertEquals(bounceUri.getPath(), getUriAttrVal(connectionOpenedSpan));
        assertEquals(initialRequestSpan.getSpanId(), connectionOpenedSpan.getLinks().get(0).getSpanContext().getSpanId());
        var connectionClosedSpan = getSpanByName("CLOSE " + bounceUri.getPath(), SpanKind.SERVER);
        assertEquals(bounceUri.getPath(), getUriAttrVal(connectionClosedSpan));
        assertEquals(BounceEndpoint.connectionId, getConnectionIdAttrVal(connectionClosedSpan));
        assertEquals(BounceEndpoint.endpointId, getEndpointIdAttrVal(connectionClosedSpan));
        assertEquals(1, connectionClosedSpan.getLinks().size());
        assertEquals(connectionOpenedSpan.getSpanId(), connectionClosedSpan.getLinks().get(0).getSpanContext().getSpanId());
    }

    private String getConnectionIdAttrVal(SpanData connectionOpenedSpan) {
        return connectionOpenedSpan
                .getAttributes()
                .get(AttributeKey.stringKey("connection.id"));
    }

    private String getClientIdAttrVal(SpanData connectionOpenedSpan) {
        return connectionOpenedSpan
                .getAttributes()
                .get(AttributeKey.stringKey("connection.client.id"));
    }

    private String getUriAttrVal(SpanData connectionOpenedSpan) {
        return connectionOpenedSpan.getAttributes().get(URL_PATH);
    }

    private String getEndpointIdAttrVal(SpanData connectionOpenedSpan) {
        return connectionOpenedSpan
                .getAttributes()
                .get(AttributeKey.stringKey("connection.endpoint.id"));
    }

    private void waitForTracesToArrive(int expectedTracesCount) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertEquals(expectedTracesCount, spanExporter.getFinishedSpanItems().size()));
    }

    private SpanData getSpanByName(String name, SpanKind kind) {
        return spanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> name.equals(sd.getName()))
                .filter(sd -> sd.getKind() == kind)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected span name '" + name + "' and kind '" + kind + "' not found: "
                                + spanExporter.getFinishedSpanItems()));
    }
}
