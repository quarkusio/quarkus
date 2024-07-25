package io.quarkus.websockets.next.runtime.telemetry;

import io.vertx.core.buffer.Buffer;

/**
 * Intercepts text or binary send from the {@link io.quarkus.websockets.next.runtime.WebSocketConnectionBase} connection.
 */
public sealed interface SendingInterceptor permits MetricsSendingInterceptor {

    // following methods should mirror WebSocketConnectionBase sending methods

    Runnable onSend(String text);

    Runnable onSend(Buffer message);

}
