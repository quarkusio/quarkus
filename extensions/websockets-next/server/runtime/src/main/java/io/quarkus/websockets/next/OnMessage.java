package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * A method of an {@link WebSocket} endpoint annotated with this annotation is invoked when an incoming message is received.
 * <p>
 * An endpoint may only declare one method annotated with this annotation.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Experimental("This API is experimental and may change in the future")
public @interface OnMessage {

    /**
     *
     * @return {@code true} if all the connected clients should receive the objects emitted by the annotated method
     * @see WebSocketServerConnection#broadcast()
     */
    public boolean broadcast() default false;

}
