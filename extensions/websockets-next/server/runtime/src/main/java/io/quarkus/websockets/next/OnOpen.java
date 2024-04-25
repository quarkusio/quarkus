package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * {@link WebSocket} and {@link WebSocketClient} endpoint methods annotated with this annotation are invoked when a new
 * connection is opened.
 * <p>
 * The method may accept the following parameters:
 * <ul>
 * <li>{@link WebSocketConnection}/{@link WebSocketClientConnection}; depending on the endpoint type</li>
 * <li>{@link HandshakeRequest}</li>
 * <li>{@link String} parameters annotated with {@link PathParam}</li>
 * </ul>
 * <p>
 * An endpoint may declare at most one method annotated with this annotation.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Experimental("This API is experimental and may change in the future")
public @interface OnOpen {

    /**
     * Broadcasting is only supported for server endpoints annotated with {@link WebSocket}.
     *
     * @return {@code true} if all the connected clients should receive the objects emitted by the annotated method
     * @see WebSocketConnection#broadcast()
     */
    public boolean broadcast() default false;

}
