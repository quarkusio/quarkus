package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.quarkus.websockets.next.WebSocketConnection.HandshakeRequest;
import io.smallrye.common.annotation.Experimental;

/**
 * A {@link WebSocket} endpoint method annotated with this annotation is invoked when the client connects to a web socket
 * endpoint.
 * <p>
 * The method may accept the following parameters:
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
public @interface OnOpen {

    /**
     * @return {@code true} if all the connected clients should receive the objects emitted by the annotated method
     * @see WebSocketConnection#broadcast()
     */
    public boolean broadcast() default false;

}
