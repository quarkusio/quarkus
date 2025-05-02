package io.quarkus.websockets.next.runtime.spi.telemetry;

/**
 * This interface must be implemented as a CDI bean by an extension that needs to collect WebSockets metrics.
 */
public interface WebSocketMetricsInterceptorProducer {

    /**
     * Creates {@link WebSocketMetricsInterceptor} for server endpoints.
     *
     * @return WebSocketMetricsInterceptor
     */
    WebSocketMetricsInterceptor createServerMetricsInterceptor();

    /**
     * Creates {@link WebSocketMetricsInterceptor} for client endpoints.
     *
     * @return WebSocketMetricsInterceptor
     */
    WebSocketMetricsInterceptor createClientMetricsInterceptor();

    interface WebSocketMetricsInterceptor {

        /**
         * Called when errors described by the 'OnError' annotation occur.
         *
         * @param route endpoint route
         */
        void onError(String route);

        /**
         * Called every time an outbound binary or text message is sent.
         *
         * @param data sent data
         * @param route endpoint route
         */
        void onMessageSent(byte[] data, String route);

        /**
         * Called every time an inbound binary or text message is received.
         *
         * @param data received data
         * @param route endpoint route
         */
        void onMessageReceived(byte[] data, String route);

        /**
         * Called when a WebSocket connection is opened.
         *
         * @param route endpoint route
         */
        void onConnectionOpened(String route);

        /**
         * Called when an opening of a WebSocket connection failed.
         *
         * @param route endpoint route
         */
        void onConnectionOpeningFailed(String route);

        /**
         * Called when a WebSocket connection is opened.
         *
         * @param route endpoint route
         */
        void onConnectionClosed(String route);

    }
}
