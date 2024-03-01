package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * The annotated method consumes/produces binary messages.
 * <p>
 * A binary message is always represented as a {@link io.vertx.core.buffer.Buffer}. Therefore, the following conversion rules
 * apply. The types listed below are handled specifically. For all other types a {@link BinaryMessageCodec} is used to encode
 * and decode input and
 * output messages. By default, the first input codec that supports the message type is used; codecs with higher priority go
 * first. However, a specific codec can be selected with {@link #inputCodec()} and {@link #outputCodec()}.
 *
 * <ul>
 * <li>{@code java.lang.Buffer} is used as is,</li>
 * <li>{@code byte[]} is encoded with {@link io.vertx.core.buffer.Buffer#buffer(byte[])} and decoded with
 * {@link io.vertx.core.buffer.Buffer#getBytes()},</li>
 * <li>{@code java.lang.String} is encoded with {@link io.vertx.core.buffer.Buffer#buffer(String)} and decoded with
 * {@link io.vertx.core.buffer.Buffer#toString()},</li>
 * <li>{@code io.vertx.core.json.JsonObject} is encoded with {@link io.vertx.core.json.JsonObject#toBuffer()} and decoded with
 * {@link io.vertx.core.json.JsonObject#JsonObject(io.vertx.core.buffer.Buffer)}.</li>
 * <li>{@code io.vertx.core.json.JsonArray} is encoded with {@link io.vertx.core.json.JsonArray#toBuffer()} and decoded with
 * {@link io.vertx.core.json.JsonArray#JsonArray(io.vertx.core.buffer.Buffer)}.</li>
 * <p>
 *
 * @see BinaryMessageCodec
 */
@Retention(RUNTIME)
@Target(METHOD)
@Experimental("This API is experimental and may change in the future")
public @interface BinaryMessage {

    /**
     * The codec used for input messages.
     * <p>
     * By default, the first codec that supports the message type is used; codecs with higher priority go first.
     * <p>
     * Note that, if specified, the codec is also used for output messages unless {@link #outputCodec()} returns a non-default
     * value.
     */
    @SuppressWarnings("rawtypes")
    Class<? extends BinaryMessageCodec> inputCodec() default BinaryMessageCodec.class;

    /**
     * The codec used for output messages.
     * <p>
     * By default, the same codec as for the input message is used.
     */
    @SuppressWarnings("rawtypes")
    Class<? extends BinaryMessageCodec> outputCodec() default BinaryMessageCodec.class;

}
