package io.quarkus.websockets.next.runtime.telemetry;

import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_CONNECTION_CLOSED;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_CONNECTION_OPENED;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_CONNECTION_OPENED_ERROR;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_ENDPOINT_COUNT_ERRORS;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.DIRECTION_TAG_KEY;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.SERVER_CONNECTION_CLOSED;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.SERVER_CONNECTION_ON_OPEN_ERROR;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.SERVER_CONNECTION_OPENED;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.SERVER_ENDPOINT_COUNT_ERRORS;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.Direction.INBOUND;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.Direction.OUTBOUND;

import java.util.function.Consumer;
import java.util.function.Function;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.quarkus.websockets.next.runtime.config.WebSocketsClientRuntimeConfig;
import io.quarkus.websockets.next.runtime.config.WebSocketsServerRuntimeConfig;
import io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.Direction;

/**
 * Installs metrics support into the WebSockets extension.
 */
public final class MetricsBuilderCustomizer implements Consumer<WebSocketTelemetryProviderBuilder> {

    private static final String URI_TAG_KEY = "uri";

    private final MeterRegistry meterRegistry;
    private final boolean serverMetricsEnabled;
    private final boolean clientMetricsEnabled;

    MetricsBuilderCustomizer(MeterRegistry meterRegistry, WebSocketsServerRuntimeConfig serverRuntimeConfig,
            WebSocketsClientRuntimeConfig clientRuntimeConfig) {
        this.serverMetricsEnabled = serverRuntimeConfig.telemetry().metricsEnabled();
        this.clientMetricsEnabled = clientRuntimeConfig.telemetry().metricsEnabled();
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void accept(WebSocketTelemetryProviderBuilder builder) {
        if (clientMetricsEnabled) {
            addClientMetricsSupport(builder);
        }
        if (serverMetricsEnabled) {
            addServerMetricsSupport(builder);
        }
    }

    private void addServerMetricsSupport(WebSocketTelemetryProviderBuilder builder) {
        final Meter.MeterProvider<Counter> messagesCounter = Counter
                .builder(TelemetryConstants.SERVER_COUNT)
                .description("Number of messages sent and received by server endpoints.")
                .withRegistry(meterRegistry);

        final Meter.MeterProvider<Counter> bytesCounter = Counter
                .builder(TelemetryConstants.SERVER_BYTES)
                .description("Number of bytes sent and received by server endpoints.")
                .withRegistry(meterRegistry);

        builder.serverEndpointDecorator(new Function<>() {

            private final Meter.MeterProvider<Counter> closedConnectionCounter = Counter
                    .builder(SERVER_CONNECTION_CLOSED)
                    .description("Number of closed server WebSocket connections.")
                    .withRegistry(meterRegistry);

            @Override
            public WebSocketEndpoint apply(TelemetryWebSocketEndpointContext ctx) {
                return new MetricsForwardingWebSocketEndpoint(ctx.endpoint(),
                        createCounter(messagesCounter, INBOUND, ctx.path()),
                        createCounter(bytesCounter, INBOUND, ctx.path()),
                        closedConnectionCounter.withTag(URI_TAG_KEY, ctx.path()));
            }
        });
        builder.pathToServerErrorInterceptor(new Function<>() {

            private final Meter.MeterProvider<Counter> serverErrorsCounter = Counter
                    .builder(SERVER_ENDPOINT_COUNT_ERRORS)
                    .description("Counts all the WebSockets server endpoint errors.")
                    .withRegistry(meterRegistry);

            @Override
            public ErrorInterceptor apply(String path) {
                return new ErrorCountingInterceptor(serverErrorsCounter.withTag(URI_TAG_KEY, path));
            }
        });
        builder.pathToServerSendingInterceptor(createSendingInterceptor(bytesCounter, messagesCounter));
        builder.pathToServerConnectionInterceptor(new Function<>() {

            private final Meter.MeterProvider<Counter> connectionOpenCounter = Counter
                    .builder(SERVER_CONNECTION_OPENED)
                    .description("Number of opened server connections.")
                    .withRegistry(meterRegistry);

            private final Meter.MeterProvider<Counter> connectionOpeningFailedCounter = Counter
                    .builder(SERVER_CONNECTION_ON_OPEN_ERROR)
                    .description("Number of failures occurred when opening server connection failed.")
                    .withRegistry(meterRegistry);

            @Override
            public ConnectionInterceptor apply(String path) {
                return new MetricsConnectionInterceptor(connectionOpenCounter.withTag(URI_TAG_KEY, path),
                        connectionOpeningFailedCounter.withTag(URI_TAG_KEY, path));
            }
        });
    }

    private void addClientMetricsSupport(WebSocketTelemetryProviderBuilder builder) {
        final Meter.MeterProvider<Counter> messagesCounter = Counter
                .builder(TelemetryConstants.CLIENT_COUNT)
                .description("Number of messages sent and received by client endpoints.")
                .withRegistry(meterRegistry);

        final Meter.MeterProvider<Counter> bytesCounter = Counter
                .builder(TelemetryConstants.CLIENT_BYTES)
                .description("Number of bytes sent and received by client endpoints.")
                .withRegistry(meterRegistry);

        builder.clientEndpointDecorator(new Function<>() {

            private final Meter.MeterProvider<Counter> closedConnectionCounter = Counter
                    .builder(CLIENT_CONNECTION_CLOSED)
                    .description("Number of closed client WebSocket connections.")
                    .withRegistry(meterRegistry);

            @Override
            public WebSocketEndpoint apply(TelemetryWebSocketEndpointContext ctx) {
                return new MetricsForwardingWebSocketEndpoint(ctx.endpoint(),
                        createCounter(messagesCounter, INBOUND, ctx.path()),
                        createCounter(bytesCounter, INBOUND, ctx.path()),
                        closedConnectionCounter.withTag(URI_TAG_KEY, ctx.path()));
            }
        });
        builder.pathToClientErrorInterceptor(new Function<>() {

            private final Meter.MeterProvider<Counter> clientErrorsCounter = Counter
                    .builder(CLIENT_ENDPOINT_COUNT_ERRORS)
                    .description("Counts all the WebSockets client endpoint errors.")
                    .withRegistry(meterRegistry);

            @Override
            public ErrorInterceptor apply(String path) {
                return new ErrorCountingInterceptor(clientErrorsCounter.withTag(URI_TAG_KEY, path));
            }
        });
        builder.pathToClientSendingInterceptor(createSendingInterceptor(bytesCounter, messagesCounter));
        builder.pathToClientConnectionInterceptor(new Function<>() {

            private final Meter.MeterProvider<Counter> connectionOpenCounter = Counter
                    .builder(CLIENT_CONNECTION_OPENED)
                    .description("Number of opened client connections.")
                    .withRegistry(meterRegistry);

            private final Meter.MeterProvider<Counter> connectionOpeningFailedCounter = Counter
                    .builder(CLIENT_CONNECTION_OPENED_ERROR)
                    .description("Number of failures occurred when opening client connection failed.")
                    .withRegistry(meterRegistry);

            @Override
            public ConnectionInterceptor apply(String path) {
                return new MetricsConnectionInterceptor(connectionOpenCounter.withTag(URI_TAG_KEY, path),
                        connectionOpeningFailedCounter.withTag(URI_TAG_KEY, path));
            }
        });
    }

    private static Function<String, SendingInterceptor> createSendingInterceptor(Meter.MeterProvider<Counter> bytesCounter,
            Meter.MeterProvider<Counter> messagesCounter) {
        return new Function<String, SendingInterceptor>() {
            @Override
            public SendingInterceptor apply(String path) {
                return new MetricsSendingInterceptor(createCounter(messagesCounter, OUTBOUND, path),
                        createCounter(bytesCounter, OUTBOUND, path));
            }
        };
    }

    private static Counter createCounter(Meter.MeterProvider<Counter> counter, Direction direction, String path) {
        return counter.withTags(URI_TAG_KEY, path, DIRECTION_TAG_KEY, direction.toString());
    }
}
