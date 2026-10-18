package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * {@link WebSocket} and {@link WebSocketClient} endpoint methods annotated with this annotation consume binary messages. An
 * endpoint may declare at most one method annotated with this annotation.
 *
 * <h2>Execution model</h2>
 *
 * <ul>
 * <li>Methods annotated with {@link io.smallrye.common.annotation.RunOnVirtualThread} are considered blocking and should be
 * executed on a virtual thread.</li>
 * <li>Methods annotated with {@link io.smallrye.common.annotation.Blocking} are considered blocking and should be executed on a
 * worker thread.</li>
 * <li>Methods annotated with {@link io.smallrye.common.annotation.NonBlocking} are considered non-blocking and should be
 * executed on an event loop thread.</li>
 * </ul>
 *
 * Execution model for methods which don't declare any of the annotation listed above is derived from the return type:
 * <p>
 * <ul>
 * <li>Methods returning {@code void} are considered blocking and should be executed on a worker thread.</li>
 * <li>Methods returning {@link io.smallrye.mutiny.Uni} or {@link io.smallrye.mutiny.Multi} are considered non-blocking and
 * should be executed on an event loop thread.</li>
 * <li>Methods returning any other type are considered blocking and should be executed on a worker thread.</li>
 * </ul>
 *
 * <h2>Method parameters</h2>
 *
 * The method must accept exactly one message parameter. A binary message is always represented as a
 * {@link io.vertx.core.buffer.Buffer}. Therefore, the following conversion rules
 * apply. The types listed below are handled specifically. For all other types a {@link BinaryMessageCodec} is used to encode
 * and decode input and output messages. By default, the first input codec that supports the message type is used; codecs with
 * higher priority go first. However, a specific codec can be selected with {@link #codec()} and {@link #outputCodec()}.
 * <p>
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
 * </ul>
 * <p>
 * The method may also accept the following parameters:
 * <ul>
 * <li>{@link WebSocketConnection}/{@link WebSocketClientConnection}; depending on the endpoint type</li>
 * <li>{@link HandshakeRequest}</li>
 * <li>{@link String} parameters annotated with {@link PathParam}</li>
 * </ul>
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface OnBinaryMessage {

    /**
     * Broadcasting is only supported for server endpoints annotated with {@link WebSocket}.
     *
     * @return {@code true} if all the connected clients should receive the objects returned by the annotated method
     * @see WebSocketConnection#broadcast()
     */
    public boolean broadcast() default false;

    /**
     * The codec used for input messages.
     * <p>
     * By default, the first codec that supports the message type is used; codecs with higher priority go first. Note that, if
     * specified, the codec is also used for output messages unless {@link #outputCodec()} returns a non-default
     * value.
     */
    @SuppressWarnings("rawtypes")
    Class<? extends BinaryMessageCodec> codec() default BinaryMessageCodec.class;

    /**
     * The codec used for output messages.
     * <p>
     * By default, the same codec as for the input message is used.
     */
    @SuppressWarnings("rawtypes")
    Class<? extends BinaryMessageCodec> outputCodec() default BinaryMessageCodec.class;

}
