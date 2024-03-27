package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * A {@link WebSocket} endpoint method annotated with this annotation is invoked when the client disconnects from the
 * socket.
 * <p>
 * An endpoint may declare at most one method annotated with this annotation.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Experimental("This API is experimental and may change in the future")
public @interface OnClose {

}
