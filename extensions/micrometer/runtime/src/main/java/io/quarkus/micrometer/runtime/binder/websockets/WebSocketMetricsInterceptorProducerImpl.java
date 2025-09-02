package io.quarkus.micrometer.runtime.binder.websockets;

import static io.quarkus.micrometer.runtime.binder.websockets.WebSocketMetricConstants.DIRECTION_TAG_KEY;
import static io.quarkus.micrometer.runtime.binder.websockets.WebSocketMetricConstants.Direction.INBOUND;
import static io.quarkus.micrometer.runtime.binder.websockets.WebSocketMetricConstants.Direction.OUTBOUND;

import jakarta.enterprise.context.Dependent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketMetricsInterceptorProducer;

@Dependent
public final class WebSocketMetricsInterceptorProducerImpl implements WebSocketMetricsInterceptorProducer {

    private static final String URI_TAG_KEY = "uri";
    private final MeterRegistry meterRegistry;

    WebSocketMetricsInterceptorProducerImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public WebSocketMetricsInterceptor createServerMetricsInterceptor() {
        final Meter.MeterProvider<Counter> messagesCounter = Counter
                .builder(WebSocketMetricConstants.SERVER_COUNT)
                .description("Number of messages sent and received by server endpoints.")
                .withRegistry(meterRegistry);
        final Meter.MeterProvider<Counter> bytesCounter = Counter
                .builder(WebSocketMetricConstants.SERVER_BYTES)
                .description("Number of bytes sent and received by server endpoints.")
                .withRegistry(meterRegistry);
        final Meter.MeterProvider<Counter> closedConnectionCounter = Counter
                .builder(WebSocketMetricConstants.SERVER_CONNECTION_CLOSED)
                .description("Number of closed server WebSocket connections.")
                .withRegistry(meterRegistry);
        final Meter.MeterProvider<Counter> serverErrorsCounter = Counter
                .builder(WebSocketMetricConstants.SERVER_ENDPOINT_COUNT_ERRORS)
                .description("Counts all the WebSockets server endpoint errors.")
                .withRegistry(meterRegistry);
        final Meter.MeterProvider<Counter> connectionOpenCounter = Counter
                .builder(WebSocketMetricConstants.SERVER_CONNECTION_OPENED)
                .description("Number of opened server connections.")
                .withRegistry(meterRegistry);
        final Meter.MeterProvider<Counter> connectionOpeningFailedCounter = Counter
                .builder(WebSocketMetricConstants.SERVER_CONNECTION_ON_OPEN_ERROR)
                .description("Number of failures occurred when opening server connection failed.")
                .withRegistry(meterRegistry);
        return new WebSocketMetricsInterceptorImpl(messagesCounter, bytesCounter, closedConnectionCounter, serverErrorsCounter,
                connectionOpenCounter, connectionOpeningFailedCounter);
    }

    @Override
    public WebSocketMetricsInterceptor createClientMetricsInterceptor() {
        final Meter.MeterProvider<Counter> messagesCounter = Counter
                .builder(WebSocketMetricConstants.CLIENT_COUNT)
                .description("Number of messages sent and received by client endpoints.")
                .withRegistry(meterRegistry);
        final Meter.MeterProvider<Counter> bytesCounter = Counter
                .builder(WebSocketMetricConstants.CLIENT_BYTES)
                .description("Number of bytes sent and received by client endpoints.")
                .withRegistry(meterRegistry);
        final Meter.MeterProvider<Counter> closedConnectionCounter = Counter
                .builder(WebSocketMetricConstants.CLIENT_CONNECTION_CLOSED)
                .description("Number of closed client WebSocket connections.")
                .withRegistry(meterRegistry);
        final Meter.MeterProvider<Counter> clientErrorsCounter = Counter
                .builder(WebSocketMetricConstants.CLIENT_ENDPOINT_COUNT_ERRORS)
                .description("Counts all the WebSockets client endpoint errors.")
                .withRegistry(meterRegistry);
        final Meter.MeterProvider<Counter> connectionOpenCounter = Counter
                .builder(WebSocketMetricConstants.CLIENT_CONNECTION_OPENED)
                .description("Number of opened client connections.")
                .withRegistry(meterRegistry);
        final Meter.MeterProvider<Counter> connectionOpeningFailedCounter = Counter
                .builder(WebSocketMetricConstants.CLIENT_CONNECTION_OPENED_ERROR)
                .description("Number of failures occurred when opening client connection failed.")
                .withRegistry(meterRegistry);
        return new WebSocketMetricsInterceptorImpl(messagesCounter, bytesCounter, closedConnectionCounter, clientErrorsCounter,
                connectionOpenCounter, connectionOpeningFailedCounter);
    }

    private static final class WebSocketMetricsInterceptorImpl implements WebSocketMetricsInterceptor {

        private final Meter.MeterProvider<Counter> messagesCounter;
        private final Meter.MeterProvider<Counter> bytesCounter;
        private final Meter.MeterProvider<Counter> closedConnectionCounter;
        private final Meter.MeterProvider<Counter> errorsCounter;
        private final Meter.MeterProvider<Counter> connectionOpenCounter;
        private final Meter.MeterProvider<Counter> connectionOpeningFailedCounter;

        private WebSocketMetricsInterceptorImpl(Meter.MeterProvider<Counter> messagesCounter,
                Meter.MeterProvider<Counter> bytesCounter, Meter.MeterProvider<Counter> closedConnectionCounter,
                Meter.MeterProvider<Counter> errorsCounter, Meter.MeterProvider<Counter> connectionOpenCounter,
                Meter.MeterProvider<Counter> connectionOpeningFailedCounter) {
            this.messagesCounter = messagesCounter;
            this.bytesCounter = bytesCounter;
            this.closedConnectionCounter = closedConnectionCounter;
            this.errorsCounter = errorsCounter;
            this.connectionOpenCounter = connectionOpenCounter;
            this.connectionOpeningFailedCounter = connectionOpeningFailedCounter;
        }

        @Override
        public void onError(String route) {
            errorsCounter.withTag(URI_TAG_KEY, route).increment();
        }

        @Override
        public void onMessageSent(byte[] data, String route) {
            messagesCounter.withTags(URI_TAG_KEY, route, DIRECTION_TAG_KEY, OUTBOUND.toString()).increment();
            bytesCounter.withTags(URI_TAG_KEY, route, DIRECTION_TAG_KEY, OUTBOUND.toString()).increment(data.length);
        }

        @Override
        public void onMessageReceived(byte[] data, String route) {
            messagesCounter.withTags(URI_TAG_KEY, route, DIRECTION_TAG_KEY, INBOUND.toString()).increment();
            bytesCounter.withTags(URI_TAG_KEY, route, DIRECTION_TAG_KEY, INBOUND.toString()).increment(data.length);
        }

        @Override
        public void onConnectionOpened(String route) {
            connectionOpenCounter.withTag(URI_TAG_KEY, route).increment();
        }

        @Override
        public void onConnectionOpeningFailed(String route) {
            connectionOpeningFailedCounter.withTag(URI_TAG_KEY, route).increment();
        }

        @Override
        public void onConnectionClosed(String route) {
            closedConnectionCounter.withTag(URI_TAG_KEY, route).increment();
        }
    }
}
