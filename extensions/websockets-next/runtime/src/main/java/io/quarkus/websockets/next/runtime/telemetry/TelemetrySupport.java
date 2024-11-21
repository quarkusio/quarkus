package io.quarkus.websockets.next.runtime.telemetry;

import java.util.Map;

import io.quarkus.websockets.next.runtime.WebSocketConnectionBase;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;

/**
 * Integrates traces into WebSockets with {@link WebSocketEndpoint} decorator.
 */
public abstract class TelemetrySupport {

    private final ConnectionInterceptor connectionInterceptor;
    private final SendingInterceptor sendingInterceptor;
    private final ErrorInterceptor errorInterceptor;

    TelemetrySupport(ConnectionInterceptor connectionInterceptor, SendingInterceptor sendingInterceptor,
            ErrorInterceptor errorInterceptor) {
        this.connectionInterceptor = connectionInterceptor;
        this.sendingInterceptor = sendingInterceptor;
        this.errorInterceptor = errorInterceptor;
    }

    public abstract WebSocketEndpoint decorate(WebSocketEndpoint endpoint, WebSocketConnectionBase connection);

    public boolean interceptConnection() {
        return connectionInterceptor != null;
    }

    /**
     * Collects telemetry when WebSocket connection is opened.
     * Only supported when {@link #interceptConnection()}.
     */
    public void connectionOpened() {
        connectionInterceptor.connectionOpened();
    }

    /**
     * Collects telemetry when WebSocket connection opening failed.
     * Only supported when {@link #interceptConnection()}.
     */
    public void connectionOpeningFailed(Throwable throwable) {
        connectionInterceptor.connectionOpeningFailed(throwable);
    }

    public SendingInterceptor getSendingInterceptor() {
        return sendingInterceptor;
    }

    public ErrorInterceptor getErrorInterceptor() {
        return errorInterceptor;
    }

    protected Map<String, Object> getContextData() {
        return connectionInterceptor == null ? Map.of() : connectionInterceptor.getContextData();
    }
}
