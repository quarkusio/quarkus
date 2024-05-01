package io.quarkus.websockets.next;

import io.smallrye.common.annotation.Experimental;

/**
 * Used to encode and decode text messages.
 *
 * @param <T>
 * @see TextMessage
 */
@Experimental("This API is experimental and may change in the future")
public interface TextMessageCodec<T> extends MessageCodec<T, String> {

}
