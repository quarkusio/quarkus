package io.quarkus.websockets.next.runtime.spi.telemetry;

import java.util.Map;

/**
 * This interface must be implemented as a CDI bean by an extension that needs to collect WebSockets traces.
 */
public interface WebSocketTracesInterceptor {

    /**
     * Called when a WebSocket connection is opened.
     * Any data that should be shared between other events (like {@link #onConnectionClosed(WebSocketEndpointContext)})
     * must be returned as a context data.
     *
     * @param route endpoint route
     * @param endpointKind whether invoked endpoint is a server endpoint or a client endpoint
     * @return context data or null (when no context data are required)
     */
    Map<String, Object> onConnectionOpened(String route, EndpointKind endpointKind);

    /**
     * Called when an opening of a WebSocket connection failed.
     *
     * @param cause {@link Throwable}
     * @param route endpoint route
     * @param endpointKind whether invoked endpoint is a server endpoint or a client endpoint
     * @param connectionOpenedContext context data produced by the {@link #onConnectionOpened(String, EndpointKind)}
     */
    void onConnectionOpeningFailed(Throwable cause, String route, EndpointKind endpointKind,
            Map<String, Object> connectionOpenedContext);

    /**
     * Called when a WebSocket connection is opened.
     *
     * @param context WebSocketEndpointContext
     */
    void onConnectionClosed(WebSocketEndpointContext context);

    /**
     * Called when a closing of a WebSocket connection failed.
     *
     * @param throwable {@link Throwable}
     * @param context endpoint context
     */
    void onConnectionClosingFailed(Throwable throwable, WebSocketEndpointContext context);

}
