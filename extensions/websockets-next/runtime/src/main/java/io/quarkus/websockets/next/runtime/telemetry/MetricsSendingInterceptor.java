package io.quarkus.websockets.next.runtime.telemetry;

import java.nio.charset.StandardCharsets;

import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketMetricsInterceptorProducer.WebSocketMetricsInterceptor;
import io.vertx.core.buffer.Buffer;

final class MetricsSendingInterceptor implements SendingInterceptor {

    private final WebSocketMetricsInterceptor interceptor;
    private final String path;

    MetricsSendingInterceptor(WebSocketMetricsInterceptor interceptor, String path) {
        this.interceptor = interceptor;
        this.path = path;
    }

    @Override
    public void onSend(String text) {
        interceptor.onMessageSent(text.getBytes(StandardCharsets.UTF_8), path);
    }

    @Override
    public void onSend(Buffer message) {
        interceptor.onMessageSent(message.getBytes(), path);
    }
}
