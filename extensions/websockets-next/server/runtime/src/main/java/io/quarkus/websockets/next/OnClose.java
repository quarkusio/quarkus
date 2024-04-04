package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.quarkus.websockets.next.WebSocketConnection.HandshakeRequest;
import io.smallrye.common.annotation.Experimental;

/**
 * A {@link WebSocket} endpoint method annotated with this annotation is invoked when the client disconnects from the
 * socket.
 * <p>
 * The method must return {@code void} or {@code io.smallrye.mutiny.Uni<Void>}.
 * The method may accept the following parameters:
 * <ul>
 * <li>{@link WebSocketConnection}</li>
 * <li>{@link HandshakeRequest}</li>
 * <li>{@link String} parameters annotated with {@link PathParam}</li>
 * </ul>
 * Note that it's not possible to send a message to the current connection as the socket is already closed when the method
 * invoked. However, it is possible to send messages to other open connections.
 * <p>
 * An endpoint may declare at most one method annotated with this annotation.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Experimental("This API is experimental and may change in the future")
public @interface OnClose {

}
