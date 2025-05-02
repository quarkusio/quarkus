package io.quarkus.websockets.next.runtime.telemetry;

import java.util.Map;

import io.quarkus.websockets.next.runtime.spi.telemetry.EndpointKind;
import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketTracesInterceptor;

final class TracesConnectionInterceptor implements ConnectionInterceptor {

    private final WebSocketTracesInterceptor tracesInterceptor;
    private final String path;
    private final EndpointKind endpointKind;
    private volatile Map<String, Object> contextData;

    TracesConnectionInterceptor(WebSocketTracesInterceptor tracesInterceptor, String path, EndpointKind endpointKind) {
        this.tracesInterceptor = tracesInterceptor;
        this.path = path;
        this.endpointKind = endpointKind;
        this.contextData = null;
    }

    @Override
    public void connectionOpened() {
        contextData = tracesInterceptor.onConnectionOpened(path, endpointKind);
    }

    @Override
    public void connectionOpeningFailed(Throwable cause) {
        tracesInterceptor.onConnectionOpeningFailed(cause, path, endpointKind, contextData);
    }

    @Override
    public Map<String, Object> getContextData() {
        return contextData == null ? Map.of() : contextData;
    }
}
