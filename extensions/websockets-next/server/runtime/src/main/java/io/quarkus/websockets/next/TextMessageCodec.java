package io.quarkus.websockets.next;

/**
 * Used to encode and decode text messages.
 *
 * @param <T>
 */
public interface TextMessageCodec<T> extends MessageCodec<T, String> {

}
