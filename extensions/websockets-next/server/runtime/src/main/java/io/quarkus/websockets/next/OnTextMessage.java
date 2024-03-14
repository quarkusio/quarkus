package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * A {@link WebSocket} endpoint method annotated with this annotation consumes text messages.
 * <p>
 * An endpoint may declare at most one method annotated with this annotation.
 * <p>
 * A text message is always represented as a {@link String}. Therefore, the following conversion rules apply. The types listed
 * below are handled specifically. For all other types a {@link TextMessageCodec} is used to encode and decode input and
 * output messages. By default, the first input codec that supports the message type is used; codecs with higher priority go
 * first. However, a specific codec can be selected with {@link #codec()} and {@link #outputCodec()}.
 *
 * <ul>
 * <li>{@code java.lang.String} is used as is,</li>
 * <li>{@code io.vertx.core.json.JsonObject} is encoded with {@link io.vertx.core.json.JsonObject#encode()} and decoded with
 * {@link io.vertx.core.json.JsonObject#JsonObject(String))}.</li>
 * <li>{@code io.vertx.core.json.JsonArray} is encoded with {@link io.vertx.core.json.JsonArray#encode()} and decoded with
 * {@link io.vertx.core.json.JsonArray#JsonArray(String))}.</li>
 * <li>{@code java.lang.Buffer} is encoded with {@link io.vertx.core.buffer.Buffer#toString()} and decoded with
 * {@link io.vertx.core.buffer.Buffer#buffer(String)},</li>
 * <li>{@code byte[]} is first converted to {@link io.vertx.core.buffer.Buffer} and then converted as defined above.</li>
 * <p>
 */
@Retention(RUNTIME)
@Target(METHOD)
@Experimental("This API is experimental and may change in the future")
public @interface OnTextMessage {

    /**
     *
     * @return {@code true} if all the connected clients should receive the objects returned by the annotated method
     * @see WebSocketConnection#broadcast()
     */
    public boolean broadcast() default false;

    /**
     * The codec used for input messages.
     * <p>
     * By default, the first codec that supports the message type is used; codecs with higher priority go first. Note that, if
     * specified, the codec is also used for output messages unless {@link #outputCodec()} returns a non-default value.
     */
    @SuppressWarnings("rawtypes")
    Class<? extends TextMessageCodec> codec() default TextMessageCodec.class;

    /**
     * The codec used for output messages.
     * <p>
     * By default, the same codec as for the input message is used.
     */
    @SuppressWarnings("rawtypes")
    Class<? extends TextMessageCodec> outputCodec() default TextMessageCodec.class;

}
