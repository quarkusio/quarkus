package io.quarkus.websockets.next.runtime.telemetry;

import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_CONNECTION_CLOSED;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_CONNECTION_OPENED;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_CONNECTION_OPENED_ERROR;
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
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.URI_ATTR_KEY;

import java.util.function.Consumer;
import java.util.function.Function;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.Arc;
import io.quarkus.websockets.next.WebSocketsClientRuntimeConfig;
import io.quarkus.websockets.next.WebSocketsServerRuntimeConfig;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;

/**
 * Installs metrics support into the WebSockets extension.
 */
public final class MetricsBuilderCustomizer implements Consumer<TelemetrySupportProviderBuilder> {
    @Override
    public void accept(TelemetrySupportProviderBuilder builder) {
        var container = Arc.container();

        var serverMetricsEnabled = container.instance(WebSocketsServerRuntimeConfig.class).get().telemetry().metricsEnabled();
        var clientMetricsEnabled = container.instance(WebSocketsClientRuntimeConfig.class).get().telemetry().metricsEnabled();
        if (clientMetricsEnabled || serverMetricsEnabled) {
            var registry = container.instance(MeterRegistry.class).get();
            if (clientMetricsEnabled) {
                addClientMetricsSupport(builder, registry);
            }
            if (serverMetricsEnabled) {
                addServerMetricsSupport(builder, registry);
            }
        }
    }

    private static void addServerMetricsSupport(TelemetrySupportProviderBuilder builder, MeterRegistry registry) {
        builder.serverEndpointDecorator(new Function<>() {

            private final Meter.MeterProvider<Counter> receivedMessagesCounter = Counter
                    .builder(SERVER_MESSAGES_COUNT_RECEIVED)
                    .description("Number of messages received by server endpoints.")
                    .withRegistry(registry);

            private final Meter.MeterProvider<Counter> receivedBytesCounter = Counter
                    .builder(SERVER_MESSAGES_COUNT_RECEIVED_BYTES)
                    .description("Number of bytes received by server endpoints.")
                    .withRegistry(registry);

            private final Meter.MeterProvider<Counter> closedConnectionCounter = Counter
                    .builder(SERVER_CONNECTION_CLOSED)
                    .description("Number of closed server WebSocket connections.")
                    .withRegistry(registry);

            @Override
            public WebSocketEndpoint apply(TelemetryWebSocketEndpointContext ctx) {
                return new MetricsWebSocketEndpointWrapper(ctx.endpoint(),
                        receivedMessagesCounter.withTag(URI_ATTR_KEY, ctx.path()),
                        receivedBytesCounter.withTag(URI_ATTR_KEY, ctx.path()),
                        closedConnectionCounter.withTag(URI_ATTR_KEY, ctx.path()));
            }
        });
        builder.pathToServerErrorInterceptor(new Function<>() {

            private final Meter.MeterProvider<Counter> serverErrorsCounter = Counter
                    .builder(SERVER_MESSAGES_COUNT_ERRORS)
                    .description("Counts all the WebSockets server endpoint errors.")
                    .withRegistry(registry);

            @Override
            public ErrorInterceptor apply(String path) {
                return new ErrorCountingInterceptor(serverErrorsCounter.withTag(URI_ATTR_KEY, path));
            }
        });
        builder.pathToServerSendingInterceptor(new Function<>() {

            private final Meter.MeterProvider<Counter> sentBytesCounter = Counter
                    .builder(SERVER_MESSAGES_COUNT_SENT_BYTES)
                    .description("Number of bytes sent from server endpoints.")
                    .withRegistry(registry);

            @Override
            public SendingInterceptor apply(String path) {
                return new MetricsSendingInterceptor(sentBytesCounter.withTag(URI_ATTR_KEY, path));
            }
        });
        builder.pathToServerConnectionInterceptor(new Function<>() {

            private final Meter.MeterProvider<Counter> connectionOpenCounter = Counter
                    .builder(SERVER_CONNECTION_OPENED)
                    .description("Number of opened server connections.")
                    .withRegistry(registry);

            private final Meter.MeterProvider<Counter> connectionOpeningFailedCounter = Counter
                    .builder(SERVER_CONNECTION_OPENED_ERROR)
                    .description("Number of failures occurred when opening server connection failed.")
                    .withRegistry(registry);

            @Override
            public ConnectionInterceptor apply(String path) {
                return new MetricsConnectionInterceptor(connectionOpenCounter.withTag(URI_ATTR_KEY, path),
                        connectionOpeningFailedCounter.withTag(URI_ATTR_KEY, path));
            }
        });
    }

    private static void addClientMetricsSupport(TelemetrySupportProviderBuilder builder, MeterRegistry registry) {
        builder.clientEndpointDecorator(new Function<>() {

            private final Meter.MeterProvider<Counter> receivedBytesCounter = Counter
                    .builder(CLIENT_MESSAGES_COUNT_RECEIVED_BYTES)
                    .description("Number of bytes received by client endpoints.")
                    .withRegistry(registry);

            private final Meter.MeterProvider<Counter> closedConnectionCounter = Counter
                    .builder(CLIENT_CONNECTION_CLOSED)
                    .description("Number of closed client WebSocket connections.")
                    .withRegistry(registry);

            @Override
            public WebSocketEndpoint apply(TelemetryWebSocketEndpointContext ctx) {
                return new MetricsWebSocketEndpointWrapper(ctx.endpoint(),
                        receivedBytesCounter.withTag(URI_ATTR_KEY, ctx.path()),
                        closedConnectionCounter.withTag(URI_ATTR_KEY, ctx.path()));
            }
        });
        builder.pathToClientErrorInterceptor(new Function<>() {

            private final Meter.MeterProvider<Counter> clientErrorsCounter = Counter
                    .builder(CLIENT_MESSAGES_COUNT_ERRORS)
                    .description("Counts all the WebSockets client endpoint errors.")
                    .withRegistry(registry);

            @Override
            public ErrorInterceptor apply(String path) {
                return new ErrorCountingInterceptor(clientErrorsCounter.withTag(URI_ATTR_KEY, path));
            }
        });
        builder.pathToClientSendingInterceptor(new Function<>() {

            private final Meter.MeterProvider<Counter> sentBytesCounter = Counter
                    .builder(CLIENT_MESSAGES_COUNT_SENT_BYTES)
                    .description("Number of bytes sent from client endpoints.")
                    .withRegistry(registry);

            private final Meter.MeterProvider<Counter> sentMessagesCounter = Counter
                    .builder(CLIENT_MESSAGES_COUNT_SENT)
                    .description("Number of messages sent from client endpoints.")
                    .withRegistry(registry);

            @Override
            public SendingInterceptor apply(String path) {
                return new MetricsSendingInterceptor(sentMessagesCounter.withTag(URI_ATTR_KEY, path),
                        sentBytesCounter.withTag(URI_ATTR_KEY, path));
            }
        });
        builder.pathToClientConnectionInterceptor(new Function<>() {

            private final Meter.MeterProvider<Counter> connectionOpenCounter = Counter
                    .builder(CLIENT_CONNECTION_OPENED)
                    .description("Number of opened client connections.")
                    .withRegistry(registry);

            private final Meter.MeterProvider<Counter> connectionOpeningFailedCounter = Counter
                    .builder(CLIENT_CONNECTION_OPENED_ERROR)
                    .description("Number of failures occurred when opening client connection failed.")
                    .withRegistry(registry);

            @Override
            public ConnectionInterceptor apply(String path) {
                return new MetricsConnectionInterceptor(connectionOpenCounter.withTag(URI_ATTR_KEY, path),
                        connectionOpeningFailedCounter.withTag(URI_ATTR_KEY, path));
            }
        });
    }
}
