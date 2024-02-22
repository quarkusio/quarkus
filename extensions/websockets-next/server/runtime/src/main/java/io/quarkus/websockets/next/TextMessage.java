package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The annotated method consumes/produces text messages.
 * <p>
 * A text message is always represented as a {@link String}. Therefore, the following conversion rules apply. The types listed
 * below are handled specifically. For all other types a {@link TextMessageCodec} is used to encode and decode input and
 * output messages. By default, the first input codec that supports the message type is used; codecs with higher priority go
 * first. However, a specific codec can be selected with {@link #inputCodec()} and {@link #outputCodec()}.
 *
 * <ul>
 * <li>{@code java.lang.String} is used as is,</li>
 * <li>{@code io.vertx.core.json.JsonObject} is encoded with {@link io.vertx.core.json.JsonObject#encode()} and decoded with
 * {@link io.vertx.core.json.JsonObject#JsonObject(String))}.</li>
 * <li>{@code io.vertx.core.json.JsonArray} is encoded with {@link io.vertx.core.json.JsonArray#encode()} and decoded with
 * {@link io.vertx.core.json.JsonArray#JsonArray(String))}.</li>
 * <li>{@code java.lang.Buffer} is encoded with {@link io.vertx.core.buffer.Buffer#toString()} and decoded with
 * {@link io.vertx.core.buffer.Buffer#buffer(String)},</li>
 * <li>{@code byte[]} is first converted to {@link io.vertx.core.buffer.Buffer} and then converted as defined above,</li>
 * <p>
 *
 * @see TextMessageCodec
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface TextMessage {

    /**
     * The codec used for input messages.
     * <p>
     * By default, the first codec that supports the message type is used; codecs with higher priority go first.
     * <p>
     * Note that, if specified, the codec is also used for output messages unless {@link #outputCodec()} returns a non-default
     * value.
     */
    @SuppressWarnings("rawtypes")
    Class<? extends TextMessageCodec> inputCodec() default TextMessageCodec.class;

    /**
     * The codec used for output messages.
     * <p>
     * By default, the same codec as for the input message is used.
     */
    @SuppressWarnings("rawtypes")
    Class<? extends TextMessageCodec> outputCodec() default TextMessageCodec.class;

}
