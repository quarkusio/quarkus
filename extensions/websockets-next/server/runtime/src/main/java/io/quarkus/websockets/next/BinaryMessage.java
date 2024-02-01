package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

/**
 * The annotated method consumes/produces binary messages.
 * <p>
 * A binary message is always represented as a {@link Buffer}. Therefore, the following conversion rules apply. The types listed
 * below are handled specifically. For all other types a {@link BinaryMessageCodec} is used to encode and decode input and
 * output messages. By default, the first input codec that can handle the message type is used; codecs with higher priority go
 * first. However, a specific codec can be selected with {@link #inputCodec()} and {@link #outputCodec()}.
 *
 * <ul>
 * <li>{@code java.lang.Buffer} is used as is,</li>
 * <li>{@code byte[]} is encoded with {@link Buffer#buffer(byte[])} and decoded with {@link Buffer#getBytes()},</li>
 * <li>{@code java.lang.String} is encoded with {@link Buffer#buffer(String)} and decoded with {@link Buffer#toString()},</li>
 * <li>{@code io.vertx.core.json.JsonObject} is encoded with {@link JsonObject#toBuffer()} and decoded with
 * {@link JsonObject#JsonObject(Buffer)}.</li>
 * <p>
 *
 * @see BinaryMessageCodec
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface BinaryMessage {

    /**
     * The codec used for input messages.
     * <p>
     * By default, the first codec that can handle the message type is used; codecs with higher priority go first.
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
