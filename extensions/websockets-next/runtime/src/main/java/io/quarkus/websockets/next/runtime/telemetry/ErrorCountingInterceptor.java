package io.quarkus.websockets.next.runtime.telemetry;

import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketMetricsInterceptorProducer.WebSocketMetricsInterceptor;

final class ErrorCountingInterceptor implements ErrorInterceptor {

    private final WebSocketMetricsInterceptor interceptor;
    private final String path;

    ErrorCountingInterceptor(WebSocketMetricsInterceptor interceptor, String path) {
        this.interceptor = interceptor;
        this.path = path;
    }

    @Override
    public void intercept(Throwable throwable) {
        interceptor.onError(path);
    }
}
