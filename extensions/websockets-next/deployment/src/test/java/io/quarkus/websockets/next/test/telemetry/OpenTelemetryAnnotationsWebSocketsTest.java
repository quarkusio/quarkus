package io.quarkus.websockets.next.test.telemetry;

import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import jakarta.annotation.security.DenyAll;
import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketConnectOptions;

public class OpenTelemetryAnnotationsWebSocketsTest {

    private static final String CUSTOM_SPAN_BOUNCE_ECHO = "custom.bounce.echo";
    private static final String CUSTOM_SPAN_ON_CLOSE = "custom.bounce.close";
    private static final String CUSTOM_SPAN_ON_ERROR = "custom.end.error";

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(OtelBounceEndpoint.class, WSClient.class, InMemorySpanExporterProducer.class,
                            OtelBounceClient.class, Endpoint.class)
                    .addAsResource(new StringAsset("""
                            quarkus.otel.bsp.export.timeout=1s
                            quarkus.otel.bsp.schedule.delay=50
                            """), "application.properties"))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-opentelemetry-deployment", Version.getVersion())));

    @TestHTTPResource("/bounce/vm")
    URI bounceUri;

    @TestHTTPResource("/")
    URI baseUri;

    @TestHTTPResource("end")
    URI endUri;

    @Inject
    Vertx vertx;

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    WebSocketConnector<OtelBounceClient> connector;

    @BeforeEach
    public void resetSpans() {
        spanExporter.reset();
        OtelBounceEndpoint.connectionId = null;
        OtelBounceEndpoint.endpointId = null;
        OtelBounceEndpoint.MESSAGES.clear();
        OtelBounceClient.MESSAGES.clear();
        OtelBounceClient.CLOSED_LATCH = new CountDownLatch(1);
        OtelBounceEndpoint.CLOSED_LATCH = new CountDownLatch(1);
    }

    @Test
    public void testServerEndpointTracesOnly() {
        assertEquals(0, spanExporter.getFinishedSpanItems().size());
        try (WSClient client = new WSClient(vertx)) {
            client.connect(new WebSocketConnectOptions(), bounceUri);
            var response = client.sendAndAwaitReply("How U Livin'").toString();
            assertEquals("How U Livin'", response);
        }
        waitForTracesToArrive(5);

        // out-of-the-box instrumentation - server traces
        var initialRequestSpan = getSpanByName("GET /bounce/:grail", SpanKind.SERVER);
        var connectionOpenedSpan = getSpanByName("OPEN /bounce/:grail", SpanKind.SERVER);
        assertEquals("/bounce/:grail", getUriAttrVal(connectionOpenedSpan));
        assertEquals(initialRequestSpan.getSpanId(), connectionOpenedSpan.getLinks().get(0).getSpanContext().getSpanId());
        var connectionClosedSpan = getSpanByName("CLOSE /bounce/:grail", SpanKind.SERVER);
        assertEquals("/bounce/:grail", getUriAttrVal(connectionClosedSpan));
        assertEquals(OtelBounceEndpoint.connectionId, getConnectionIdAttrVal(connectionClosedSpan));
        assertEquals(OtelBounceEndpoint.endpointId, getEndpointIdAttrVal(connectionClosedSpan));
        assertEquals(1, connectionClosedSpan.getLinks().size());
        assertEquals(connectionOpenedSpan.getSpanId(), connectionClosedSpan.getLinks().get(0).getSpanContext().getSpanId());

        // custom span added as the server endpoint `@OnTextMessage` method is annotated with the @WithSpan
        var customBounceServerSpan = getSpanByName(CUSTOM_SPAN_BOUNCE_ECHO, SpanKind.SERVER);
        assertThat(customBounceServerSpan.getAttributes().get(AttributeKey.stringKey("code.function.name")))
                .endsWith("OtelBounceEndpoint.serverEcho");
        // test that it is possible to add SpanAttribute to the onMessage method allowed arguments
        assertEquals("How U Livin'",
                customBounceServerSpan.getAttributes().get(AttributeKey.stringKey("server-message-callback-param")));
        assertThat(customBounceServerSpan.getAttributes().get(AttributeKey.stringKey("connection")))
                // WebConnection toString contains endpoint id, path, ...
                .contains("bounce-server-endpoint-id")
                .contains("/bounce/vm");
        assertThat(customBounceServerSpan.getAttributes().get(AttributeKey.stringKey("handshake-request")))
                .contains("HandshakeRequest");
        assertThat(customBounceServerSpan.getAttributes().get(AttributeKey.stringKey("path-param"))).isEqualTo("vm");
        var onCloseSpan = getSpanByName(CUSTOM_SPAN_ON_CLOSE, SpanKind.SERVER);
        assertThat(onCloseSpan.getAttributes().get(AttributeKey.stringKey("close-reason"))).contains("code=1000");
    }

    @Test
    public void testClientAndServerEndpointTraces() throws InterruptedException {
        var clientConn = connector.baseUri(baseUri).pathParam("grail", "vm").connectAndAwait();
        clientConn.sendTextAndAwait("Make It Bun Dem");

        // assert client and server called
        Awaitility.await().untilAsserted(() -> {
            assertEquals(1, OtelBounceEndpoint.MESSAGES.size());
            assertEquals("Make It Bun Dem", OtelBounceEndpoint.MESSAGES.get(0));
            assertEquals(1, OtelBounceClient.MESSAGES.size());
            assertEquals("Make It Bun Dem", OtelBounceClient.MESSAGES.get(0));
        });

        clientConn.closeAndAwait();
        // assert connection closed and client/server were notified
        assertTrue(OtelBounceClient.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(OtelBounceEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));

        waitForTracesToArrive(8);

        // out-of-the-box instrumentation - server traces
        var initialRequestSpan = getSpanByName("GET /bounce/:grail", SpanKind.SERVER);
        var connectionOpenedSpan = getSpanByName("OPEN /bounce/:grail", SpanKind.SERVER);
        assertEquals("/bounce/:grail", getUriAttrVal(connectionOpenedSpan));
        assertEquals(initialRequestSpan.getSpanId(), connectionOpenedSpan.getLinks().get(0).getSpanContext().getSpanId());
        var connectionClosedSpan = getSpanByName("CLOSE /bounce/:grail", SpanKind.SERVER);
        assertEquals("/bounce/:grail", getUriAttrVal(connectionClosedSpan));
        assertEquals(OtelBounceEndpoint.connectionId, getConnectionIdAttrVal(connectionClosedSpan));
        assertEquals(OtelBounceEndpoint.endpointId, getEndpointIdAttrVal(connectionClosedSpan));
        assertEquals(1, connectionClosedSpan.getLinks().size());
        assertEquals(connectionOpenedSpan.getSpanId(), connectionClosedSpan.getLinks().get(0).getSpanContext().getSpanId());

        // custom span added as the server endpoint `@OnTextMessage` method is annotated with the @WithSpan
        var customBounceServerSpan = getSpanByName(CUSTOM_SPAN_BOUNCE_ECHO, SpanKind.SERVER);
        assertThat(customBounceServerSpan.getAttributes().get(AttributeKey.stringKey("code.function.name")))
                .endsWith("OtelBounceEndpoint.serverEcho");
        // test that it is possible to add SpanAttribute to the onMessage method allowed arguments
        assertEquals("Make It Bun Dem",
                customBounceServerSpan.getAttributes().get(AttributeKey.stringKey("server-message-callback-param")));
        assertThat(customBounceServerSpan.getAttributes().get(AttributeKey.stringKey("connection")))
                // WebConnection toString contains endpoint id, path, ...
                .contains("bounce-server-endpoint-id")
                .contains("/bounce/vm");
        assertThat(customBounceServerSpan.getAttributes().get(AttributeKey.stringKey("handshake-request")))
                .contains("HandshakeRequest");
        assertThat(customBounceServerSpan.getAttributes().get(AttributeKey.stringKey("path-param"))).isEqualTo("vm");
        var onCloseSpan = getSpanByName(CUSTOM_SPAN_ON_CLOSE, SpanKind.SERVER);
        assertThat(onCloseSpan.getAttributes().get(AttributeKey.stringKey("close-reason"))).contains("code=1000");

        // out-of-the-box instrumentation - client traces
        connectionOpenedSpan = getSpanByName("OPEN /bounce/{grail}", SpanKind.CLIENT);
        assertEquals("/bounce/{grail}", getUriAttrVal(connectionOpenedSpan));
        assertTrue(connectionOpenedSpan.getLinks().isEmpty());
        connectionClosedSpan = getSpanByName("CLOSE /bounce/{grail}", SpanKind.CLIENT);
        assertEquals("/bounce/{grail}", getUriAttrVal(connectionClosedSpan));
        assertNotNull(getConnectionIdAttrVal(connectionClosedSpan));
        assertNotNull(getClientIdAttrVal(connectionClosedSpan));
        assertEquals(1, connectionClosedSpan.getLinks().size());
        assertEquals(connectionOpenedSpan.getSpanId(), connectionClosedSpan.getLinks().get(0).getSpanContext().getSpanId());

        // custom span added as the client endpoint `@OnTextMessage` method is annotated with the @WithSpan
        var customBounceClientSpan = getSpanByName(CUSTOM_SPAN_BOUNCE_ECHO, SpanKind.CLIENT);
        assertThat(customBounceClientSpan.getAttributes().get(AttributeKey.stringKey("code.function.name")))
                .endsWith("OtelBounceClient.clientEcho");
        // test that it is possible to add SpanAttribute to the onMessage method allowed arguments
        assertEquals("Make It Bun Dem",
                customBounceClientSpan.getAttributes().get(AttributeKey.stringKey("client-message-callback-param")));
        assertThat(customBounceClientSpan.getAttributes().get(AttributeKey.stringKey("connection")))
                .contains("bounce-client-id");
        assertThat(customBounceClientSpan.getAttributes().get(AttributeKey.stringKey("handshake-request")))
                .contains("HandshakeRequest");
        assertThat(customBounceClientSpan.getAttributes().get(AttributeKey.stringKey("path-param"))).contains("vm");
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
        waitForTracesToArrive(7);

        // out-of-the-box instrumentation - server traces
        var initialRequestSpan = getSpanByName("GET /bounce/:grail", SpanKind.SERVER);
        var connectionOpenedSpan = getSpanByName("OPEN /bounce/:grail", SpanKind.SERVER);
        assertEquals("/bounce/:grail", getUriAttrVal(connectionOpenedSpan));
        assertEquals(initialRequestSpan.getSpanId(), connectionOpenedSpan.getLinks().get(0).getSpanContext().getSpanId());
        var connectionClosedSpan = getSpanByName("CLOSE /bounce/:grail", SpanKind.SERVER);
        assertEquals("/bounce/:grail", getUriAttrVal(connectionClosedSpan));
        assertEquals(OtelBounceEndpoint.connectionId, getConnectionIdAttrVal(connectionClosedSpan));
        assertEquals(OtelBounceEndpoint.endpointId, getEndpointIdAttrVal(connectionClosedSpan));
        assertEquals(1, connectionClosedSpan.getLinks().size());
        assertEquals(connectionOpenedSpan.getSpanId(), connectionClosedSpan.getLinks().get(0).getSpanContext().getSpanId());

        // custom span added as the server endpoint `@OnTextMessage` method is annotated with the @WithSpan
        assertThat(getSpansByName(CUSTOM_SPAN_BOUNCE_ECHO, SpanKind.SERVER).toList())
                .hasSize(3)
                .allMatch(span -> {
                    var callbackParam = span.getAttributes().get(AttributeKey.stringKey("server-message-callback-param"));
                    return List.of("throw-exception", "I'm Only Bleeding", "It's Alright, Ma").contains(callbackParam);
                })
                .allMatch(span -> {
                    String codeFunctionName = span.getAttributes().get(AttributeKey.stringKey("code.function.name"));
                    return codeFunctionName != null && codeFunctionName.endsWith("OtelBounceEndpoint.serverEcho");
                });
        var onCloseSpan = getSpanByName(CUSTOM_SPAN_ON_CLOSE, SpanKind.SERVER);
        assertThat(onCloseSpan.getAttributes().get(AttributeKey.stringKey("close-reason")))
                .contains("code=1011")
                .contains("Failing 'serverEcho' to test behavior when an exception was thrown");
    }

    @Test
    public void testSpanAttributeOnError() {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(endUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("unauthorized", client.getMessages().get(1).toString());
        }

        waitForTracesToArrive(4);

        // out-of-the-box instrumentation - server traces
        var connectionOpenedSpan = getSpanByName("OPEN /end", SpanKind.SERVER);
        assertEquals("/end", getUriAttrVal(connectionOpenedSpan));
        var getSpan = getSpanByName("GET /end", SpanKind.SERVER);
        assertEquals("/end", getUriAttrVal(getSpan));
        var connectionClosedSpan = getSpanByName("CLOSE /end", SpanKind.SERVER);
        assertEquals("/end", getUriAttrVal(connectionClosedSpan));

        // custom span added as the server endpoint `@OnError` method is annotated with the @WithSpan
        var onErrorSpan = getSpanByName(CUSTOM_SPAN_ON_ERROR, SpanKind.SERVER);
        assertThat(onErrorSpan.getAttributes().get(AttributeKey.stringKey("code.function.name")))
                .endsWith("Endpoint.error");
        // custom attribute for the error callback argument
        assertEquals(UnauthorizedException.class.getName(),
                onErrorSpan.getAttributes().get(AttributeKey.stringKey("custom.error")));
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
                .untilAsserted(() -> assertEquals(expectedTracesCount, spanExporter.getFinishedSpanItems().size(),
                        () -> "expected " + expectedTracesCount + " spans to arrive, got "
                                + spanExporter.getFinishedSpanItems().size() + " spans: "
                                + spanExporter.getFinishedSpanItems().toString()));
    }

    private SpanData getSpanByName(String name, SpanKind kind) {
        return getSpansByName(name, kind)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected span name '" + name + "' and kind '" + kind + "' not found: "
                                + spanExporter.getFinishedSpanItems()));
    }

    private Stream<SpanData> getSpansByName(String name, SpanKind kind) {
        return spanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> name.equals(sd.getName()))
                .filter(sd -> sd.getKind() == kind);
    }

    @WebSocket(path = "/bounce/{grail}", endpointId = "bounce-server-endpoint-id")
    public static class OtelBounceEndpoint {

        public static final List<String> MESSAGES = new CopyOnWriteArrayList<>();
        public static volatile CountDownLatch CLOSED_LATCH = new CountDownLatch(1);
        public static volatile String connectionId = null;
        public static volatile String endpointId = null;

        @ConfigProperty(name = "bounce-endpoint.prefix-responses", defaultValue = "false")
        boolean prefixResponses;

        @WithSpan(value = CUSTOM_SPAN_BOUNCE_ECHO, kind = SpanKind.SERVER)
        @OnTextMessage
        public String serverEcho(@SpanAttribute("server-message-callback-param") String message,
                @SpanAttribute("handshake-request") HandshakeRequest handshakeRequest,
                @SpanAttribute("connection") WebSocketConnection connection,
                @SpanAttribute("path-param") @PathParam("grail") String grailValue) {
            if (prefixResponses) {
                message = "echo 0: " + message;
            }
            MESSAGES.add(message);
            if (message.equals("throw-exception")) {
                throw new RuntimeException("Failing 'serverEcho' to test behavior when an exception was thrown");
            }
            return message;
        }

        @OnOpen
        void open(WebSocketConnection connection) {
            connectionId = connection.id();
            endpointId = connection.endpointId();
        }

        @WithSpan(value = CUSTOM_SPAN_ON_CLOSE, kind = SpanKind.SERVER)
        @OnClose
        void onClose(@SpanAttribute("close-reason") CloseReason closeReason) {
            CLOSED_LATCH.countDown();
        }

    }

    @WebSocketClient(path = "/bounce/{grail}", clientId = "bounce-client-id")
    public static class OtelBounceClient {

        public static final List<String> MESSAGES = new CopyOnWriteArrayList<>();
        public static volatile CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @WithSpan(value = CUSTOM_SPAN_BOUNCE_ECHO, kind = SpanKind.CLIENT)
        @OnTextMessage
        void clientEcho(@SpanAttribute("client-message-callback-param") String message,
                @SpanAttribute("handshake-request") HandshakeRequest handshakeRequest,
                @SpanAttribute("connection") WebSocketClientConnection connection,
                @SpanAttribute("path-param") @PathParam("grail") String grailValue) {
            MESSAGES.add(message);
        }

        @OnClose
        void onClose() {
            CLOSED_LATCH.countDown();
        }

    }

    @WebSocket(path = "/end")
    public static class Endpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @DenyAll
        @OnTextMessage
        String echo(String message) {
            return message;
        }

        @WithSpan(value = CUSTOM_SPAN_ON_ERROR, kind = SpanKind.SERVER)
        @OnError
        String error(@SpanAttribute("custom.error") UnauthorizedException t) {
            return "unauthorized";
        }

    }
}
