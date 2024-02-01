package io.quarkus.websockets.next;

import io.vertx.core.buffer.Buffer;

/**
 * Used to encode and decode binary messages.
 *
 * @param <T>
 */
public interface BinaryMessageCodec<T> extends MessageCodec<T, Buffer> {

}
