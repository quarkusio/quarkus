package io.quarkus.websockets.next;

import io.vertx.core.buffer.Buffer;

/**
 * Sends a message to the connected WebSocket client and waits for the completion.
 * <p>
 * Note that blocking sender methods should never be called on an event loop thread.
 */
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
}
