package io.quarkus.websockets.next;

import io.smallrye.common.annotation.Experimental;
import io.vertx.core.buffer.Buffer;

/**
 * Used to encode and decode binary messages.
 *
 * @param <T>
 * @see BinaryMessage
 */
@Experimental("This API is experimental and may change in the future")
public interface BinaryMessageCodec<T> extends MessageCodec<T, Buffer> {

}
