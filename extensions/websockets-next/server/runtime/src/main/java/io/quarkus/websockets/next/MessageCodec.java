package io.quarkus.websockets.next;

import java.lang.reflect.Type;

import io.smallrye.common.annotation.Experimental;

/**
 * Used to encode and decode messages.
 *
 * <h2>Special types of messages</h2>
 * Some types of messages bypass the encoding/decoding process:
 * <ul>
 * <li>{@code java.lang.Buffer},</li>
 * <li>{@code byte[]},</li>
 * <li>{@code java.lang.String},</li>
 * <li>{@code io.vertx.core.json.JsonObject}.</li>
 * <li>{@code io.vertx.core.json.JsonArray}.</li>
 * </ul>
 * The encoding/decoding details are described in {@link BinaryMessage} and {@link TextMessage}.
 *
 * <h2>CDI beans</h2>
 * Implementation classes must be CDI beans. Qualifiers are ignored. {@link jakarta.enterprise.context.Dependent} beans are
 * reused during encoding/decoding.
 *
 * <h2>Lifecycle and concurrency</h2>
 * Codecs are shared accross all WebSocket connections. Therefore, implementations should be either stateless or thread-safe.
 *
 * @param <T>
 * @param <MESSAGE>
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
