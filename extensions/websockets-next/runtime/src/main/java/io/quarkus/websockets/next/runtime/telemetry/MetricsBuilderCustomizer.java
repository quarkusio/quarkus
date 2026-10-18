package io.quarkus.websockets.next.runtime.telemetry;

import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;

import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.quarkus.websockets.next.runtime.config.WebSocketsClientRuntimeConfig;
import io.quarkus.websockets.next.runtime.config.WebSocketsServerRuntimeConfig;
import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketMetricsInterceptorProducer;
import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketMetricsInterceptorProducer.WebSocketMetricsInterceptor;

/**
 * Installs metrics support into the WebSockets extension.
 */
public final class MetricsBuilderCustomizer implements Consumer<WebSocketTelemetryProviderBuilder> {

    private final Instance<WebSocketMetricsInterceptorProducer> interceptorProducerInstance;
    private final boolean serverMetricsEnabled;
    private final boolean clientMetricsEnabled;

    MetricsBuilderCustomizer(WebSocketsServerRuntimeConfig serverRuntimeConfig,
            WebSocketsClientRuntimeConfig clientRuntimeConfig,
            Instance<WebSocketMetricsInterceptorProducer> interceptorProducerInstance) {
        this.serverMetricsEnabled = serverRuntimeConfig.telemetry().metricsEnabled();
        this.clientMetricsEnabled = clientRuntimeConfig.telemetry().metricsEnabled();
        this.interceptorProducerInstance = interceptorProducerInstance;
    }

    @Override
    public void accept(WebSocketTelemetryProviderBuilder builder) {
        if (interceptorProducerInstance.isResolvable()) {
            final WebSocketMetricsInterceptorProducer interceptorProducer = interceptorProducerInstance.get();
            if (clientMetricsEnabled) {
                addClientMetricsSupport(builder, interceptorProducer);
            }
            if (serverMetricsEnabled) {
                addServerMetricsSupport(builder, interceptorProducer);
            }
        }
    }

    private static void addServerMetricsSupport(WebSocketTelemetryProviderBuilder builder,
            WebSocketMetricsInterceptorProducer interceptorProducer) {
        final WebSocketMetricsInterceptor interceptor = interceptorProducer.createServerMetricsInterceptor();
        builder.serverEndpointDecorator(new Function<>() {
            @Override
            public WebSocketEndpoint apply(TelemetryWebSocketEndpointContext ctx) {
                return new MetricsForwardingWebSocketEndpoint(ctx.endpoint(), interceptor, ctx.path());
            }
        });
        builder.pathToServerErrorInterceptor(new Function<>() {
            @Override
            public ErrorInterceptor apply(String path) {
                return new ErrorCountingInterceptor(interceptor, path);
            }
        });
        builder.pathToServerSendingInterceptor(new Function<String, SendingInterceptor>() {
            @Override
            public SendingInterceptor apply(String path) {
                return new MetricsSendingInterceptor(interceptor, path);
            }
        });
        builder.pathToServerConnectionInterceptor(new Function<>() {
            @Override
            public ConnectionInterceptor apply(String path) {
                return new MetricsConnectionInterceptor(interceptor, path);
            }
        });
    }

    private static void addClientMetricsSupport(WebSocketTelemetryProviderBuilder builder,
            WebSocketMetricsInterceptorProducer interceptorProducer) {
        final WebSocketMetricsInterceptor interceptor = interceptorProducer.createClientMetricsInterceptor();
        builder.clientEndpointDecorator(new Function<>() {
            @Override
            public WebSocketEndpoint apply(TelemetryWebSocketEndpointContext ctx) {
                return new MetricsForwardingWebSocketEndpoint(ctx.endpoint(), interceptor, ctx.path());
            }
        });
        builder.pathToClientErrorInterceptor(new Function<>() {
            @Override
            public ErrorInterceptor apply(String path) {
                return new ErrorCountingInterceptor(interceptor, path);
            }
        });
        builder.pathToClientSendingInterceptor(new Function<String, SendingInterceptor>() {
            @Override
            public SendingInterceptor apply(String path) {
                return new MetricsSendingInterceptor(interceptor, path);
            }
        });
        builder.pathToClientConnectionInterceptor(new Function<>() {
            @Override
            public ConnectionInterceptor apply(String path) {
                return new MetricsConnectionInterceptor(interceptor, path);
            }
        });
    }
}
