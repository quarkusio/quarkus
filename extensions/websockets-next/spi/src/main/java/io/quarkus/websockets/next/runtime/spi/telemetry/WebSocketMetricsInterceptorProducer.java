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
         * @param path endpoint path
         */
        void onError(String path);

        /**
         * Called every time an outbound binary or text message is sent.
         *
         * @param data sent data
         * @param path endpoint path
         */
        void onMessageSent(byte[] data, String path);

        /**
         * Called every time an inbound binary or text message is received.
         *
         * @param data received data
         * @param path endpoint path
         */
        void onMessageReceived(byte[] data, String path);

        /**
         * Called when a WebSocket connection is opened.
         *
         * @param path endpoint path
         */
        void onConnectionOpened(String path);

        /**
         * Called when an opening of a WebSocket connection failed.
         *
         * @param path endpoint path
         */
        void onConnectionOpeningFailed(String path);

        /**
         * Called when a WebSocket connection is opened.
         *
         * @param path endpoint path
         */
        void onConnectionClosed(String path);

    }
}
