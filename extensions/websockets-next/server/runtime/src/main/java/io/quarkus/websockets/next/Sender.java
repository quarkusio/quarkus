package io.quarkus.websockets.next;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;

/**
 * Sends a message to the connected WebSocket client.
 */
@Experimental("This API is experimental and may change in the future")
public interface Sender {

    /**
     * Send a text message.
     *
     * @param message
     * @return a new {@link Uni} with a {@code null} item
     */
    @CheckReturnValue
    Uni<Void> sendText(String message);

    /**
     * Send a text message.
     *
     * @param <M>
     * @param message
     * @return a new {@link Uni} with a {@code null} item
     */
    @CheckReturnValue
    <M> Uni<Void> sendText(M message);

    /**
     * Send a binary message.
     *
     * @param message
     * @return a new {@link Uni} with a {@code null} item
     */
    @CheckReturnValue
    Uni<Void> sendBinary(Buffer message);

    /**
     * Send a binary message.
     *
     * @param message
     */
    @CheckReturnValue
    default Uni<Void> sendBinary(byte[] message) {
        return sendBinary(Buffer.buffer(message));
    }

}
