package io.quarkus.websockets.next.runtime.telemetry;

import java.util.Map;

import io.quarkus.websockets.next.runtime.WebSocketConnectionBase;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;

/**
 * Integrates metrics and traces into WebSockets with {@link SendingInterceptor}, {@link ErrorInterceptor}
 * and {@link WebSocketEndpoint} decorator.
 */
public abstract class TelemetrySupport {

    static final TelemetrySupport EMPTY = new TelemetrySupport(null, null, null) {

        @Override
        public WebSocketEndpoint decorate(WebSocketEndpoint endpoint, WebSocketConnectionBase connection) {
            return endpoint;
        }

        @Override
        public boolean interceptConnection() {
            return false;
        }
    };

    private final SendingInterceptor sendingInterceptor;
    private final ErrorInterceptor errorInterceptor;
    private final ConnectionInterceptor connectionInterceptor;

    TelemetrySupport(SendingInterceptor sendingInterceptor, ErrorInterceptor errorInterceptor,
            ConnectionInterceptor connectionInterceptor) {
        this.sendingInterceptor = sendingInterceptor;
        this.errorInterceptor = errorInterceptor;
        this.connectionInterceptor = connectionInterceptor;
    }

    public abstract WebSocketEndpoint decorate(WebSocketEndpoint endpoint, WebSocketConnectionBase connection);

    public SendingInterceptor getSendingInterceptor() {
        return sendingInterceptor;
    }

    public ErrorInterceptor getErrorInterceptor() {
        return errorInterceptor;
    }

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

    protected Map<String, Object> getContextData() {
        return connectionInterceptor == null ? Map.of() : connectionInterceptor.getContextData();
    }
}
