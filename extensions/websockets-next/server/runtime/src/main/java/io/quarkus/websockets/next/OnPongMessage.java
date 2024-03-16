package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * A {@link WebSocket} endpoint method annotated with this annotation consumes pong messages.
 * <p>
 * An endpoint may declare at most one method annotated with this annotation.
 * <p>
 * A pong message is always represented as a {@link io.vertx.core.buffer.Buffer}.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Experimental("This API is experimental and may change in the future")
public @interface OnPongMessage {

}
