package io.quarkus.websockets.next.runtime.telemetry;

import java.util.Map;

import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketMetricsInterceptorProducer.WebSocketMetricsInterceptor;

final class MetricsConnectionInterceptor implements ConnectionInterceptor {

    private final WebSocketMetricsInterceptor interceptor;
    private final String path;

    MetricsConnectionInterceptor(WebSocketMetricsInterceptor interceptor, String path) {
        this.interceptor = interceptor;
        this.path = path;
    }

    @Override
    public void connectionOpened() {
        interceptor.onConnectionOpened(path);
    }

    @Override
    public void connectionOpeningFailed(Throwable cause) {
        interceptor.onConnectionOpeningFailed(path);
    }

    @Override
    public Map<String, Object> getContextData() {
        return Map.of();
    }
}
