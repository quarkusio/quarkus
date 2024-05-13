package io.quarkus.websockets.next;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;

/**
 * Sends messages to the connected WebSocket client.
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
     * @return a new {@link Uni} with a {@code null} item
     */
    @CheckReturnValue
    default Uni<Void> sendBinary(byte[] message) {
        return sendBinary(Buffer.buffer(message));
    }

    /**
     * Send a ping message.
     *
     * @param data May be at most 125 bytes
     * @return a new {@link Uni} with a {@code null} item
     */
    @CheckReturnValue
    Uni<Void> sendPing(Buffer data);

    /**
     * Send an unsolicited pong message.
     * <p>
     * Note that the server automatically responds to a ping message sent from the client. However, the RFC 6455
     * <a href="https://tools.ietf.org/html/rfc6455#section-5.5.3">section 5.5.3</a> states that unsolicited pong may serve as a
     * unidirectional heartbeat.
     *
     * @param data May be at most 125 bytes
     * @return a new {@link Uni} with a {@code null} item
     */
    @CheckReturnValue
    Uni<Void> sendPong(Buffer data);

}
