package io.quarkus.websockets.next;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;

/**
 * Sends messages to the connected WebSocket client/server.
 */
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
     * Sends a text message and waits for the completion.
     * <p>
     * This method should never be called on an event loop thread.
     *
     * @param message
     */
    default void sendTextAndAwait(String message) {
        sendText(message).await().indefinitely();
    }

    /**
     * Send a text message.
     * <p>
     * A {@link TextMessageCodec} is used to encode the message.
     *
     * @param <M>
     * @param message
     * @return a new {@link Uni} with a {@code null} item
     * @see TextMessageCodec
     */
    @CheckReturnValue
    <M> Uni<Void> sendText(M message);

    /**
     * Sends a text message and waits for the completion.
     * <p>
     * A {@link TextMessageCodec} is used to encode the message.
     * <p>
     * This method should never be called on an event loop thread.
     *
     * @param <M>
     * @param message
     * @see TextMessageCodec
     */
    default <M> void sendTextAndAwait(M message) {
        sendText(message).await().indefinitely();
    }

    /**
     * Send a binary message.
     *
     * @param message
     * @return a new {@link Uni} with a {@code null} item
     */
    @CheckReturnValue
    Uni<Void> sendBinary(Buffer message);

    /**
     * Sends a binary message and waits for the completion.
     * <p>
     * This method should never be called on an event loop thread.
     *
     * @param message
     */
    default void sendBinaryAndAwait(Buffer message) {
        sendBinary(message).await().indefinitely();
    }

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
     * Sends a binary message and waits for the completion.
     * <p>
     * This method should never be called on an event loop thread.
     *
     * @param message
     */
    default void sendBinaryAndAwait(byte[] message) {
        sendBinary(message).await().indefinitely();
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
     * Send a ping message and waits for the completion.
     * <p>
     * This method should never be called on an event loop thread.
     *
     * @param data May be at most 125 bytes
     */
    default void sendPingAndAwait(Buffer data) {
        sendPing(data).await().indefinitely();
    }

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

    /**
     * Send an unsolicited pong message and waits for the completion.
     * <p>
     * This method should never be called on an event loop thread.
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
