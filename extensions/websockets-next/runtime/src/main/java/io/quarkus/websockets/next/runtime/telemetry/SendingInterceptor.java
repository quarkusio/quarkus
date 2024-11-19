package io.quarkus.websockets.next.runtime.telemetry;

import io.vertx.core.buffer.Buffer;

/**
 * Intercepts text and binary messages sent from the {@link io.quarkus.websockets.next.runtime.WebSocketConnectionBase}
 * connection.
 */
public sealed interface SendingInterceptor permits MetricsSendingInterceptor {

    /**
     * Intercept sent text messages, corresponds
     * to the {@link io.quarkus.websockets.next.runtime.WebSocketConnectionBase#sendText(String)} method.
     *
     * @param textMessage sent text message
     */
    void onSend(String textMessage);

    /**
     * Intercept sent binary messages, corresponds
     * to the {@link io.quarkus.websockets.next.runtime.WebSocketConnectionBase#sendBinary(Buffer)} method.
     *
     * @param binaryMessage sent binary message
     */
    void onSend(Buffer binaryMessage);

}
