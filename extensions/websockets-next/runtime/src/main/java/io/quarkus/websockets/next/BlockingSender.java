package io.quarkus.websockets.next;

import io.smallrye.common.annotation.Experimental;
import io.vertx.core.buffer.Buffer;

/**
 * Sends messages to the connected WebSocket client and waits for the completion.
 * <p>
 * Note that blocking sender methods should never be called on an event loop thread.
 */
@Experimental("This API is experimental and may change in the future")
public interface BlockingSender extends Sender {

    /**
     * Sends a text message and waits for the completion.
     *
     * @param message
     */
    default void sendTextAndAwait(String message) {
        sendText(message).await().indefinitely();
    }

    /**
     * Sends a text message and waits for the completion.
     *
     * @param <M>
     * @param message
     */
    default <M> void sendTextAndAwait(M message) {
        sendText(message).await().indefinitely();
    }

    /**
     * Sends a binary message and waits for the completion.
     *
     * @param message
     */
    default void sendBinaryAndAwait(Buffer message) {
        sendBinary(message).await().indefinitely();
    }

    /**
     * Sends a binary message and waits for the completion.
     *
     * @param message
     */
    default void sendBinaryAndAwait(byte[] message) {
        sendBinary(message).await().indefinitely();
    }

    /**
     * Send a ping message and waits for the completion.
     *
     * @param data May be at most 125 bytes
     */
    default void sendPingAndAwait(Buffer data) {
        sendPing(data).await().indefinitely();
    }

    /**
     * Send an unsolicited pong message and waits for the completion.
     * <p>
     * Note that the server automatically responds to a ping message sent from the client. However, the RFC 6455
     * <a href="https://tools.ietf.org/html/rfc6455#section-5.5.3">section 5.5.3</a> states that unsolicited pong may serve as a
     * unidirectional heartbeat.
     *
     * @param data May be at most 125 bytes
     */
    default void sendPongAndAwait(Buffer data) {
        sendPong(data).await().indefinitely();
    }
}
