package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.quarkus.websockets.next.WebSocketConnection.HandshakeRequest;
import io.smallrye.common.annotation.Experimental;

/**
 * A {@link WebSocket} endpoint method annotated with this annotation consumes pong messages.
 *
 * The method must accept exactly one pong message parameter represented as a {@link io.vertx.core.buffer.Buffer}. The method
 * may also accept the following parameters:
 * <ul>
 * <li>{@link WebSocketConnection}</li>
 * <li>{@link HandshakeRequest}</li>
 * <li>{@link String} parameters annotated with {@link PathParam}</li>
 * </ul>
 * <p>
 * An endpoint may declare at most one method annotated with this annotation.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Experimental("This API is experimental and may change in the future")
public @interface OnPongMessage {

}
