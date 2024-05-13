package io.quarkus.websockets.next;

import java.lang.reflect.Type;

import io.smallrye.common.annotation.Experimental;

/**
 * Used to encode and decode messages.
 *
 * @param <T>
 * @param <MESSAGE>
 * @see TextMessageCodec
 * @see BinaryMessageCodec
 */
@Experimental("This API is experimental and may change in the future")
public interface MessageCodec<T, MESSAGE> {

    /**
     *
     * @param type the type to handle, must not be {@code null}
     * @return {@code true} if this codec can encode/decode the provided type, {@code false} otherwise
     */
    boolean supports(Type type);

    /**
     *
     * @param value the value to encode, must not be {@code null}
     * @return the encoded representation of the value
     */
    MESSAGE encode(T value);

    /**
     *
     * @param type the type of the object to decode, must not be {@code null}
     * @param value the value to decode, must not be {@code null}
     * @return the decoded representation of the value
     */
    T decode(Type type, MESSAGE value);

}
