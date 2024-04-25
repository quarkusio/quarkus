package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * {@link WebSocket} and {@link WebSocketClient} endpoint methods annotated with this annotation are invoked when a connection
 * is closed.
 * <p>
 * The method must return {@code void} or {@code io.smallrye.mutiny.Uni<Void>}.
 * The method may accept the following parameters:
 * <ul>
 * <li>{@link WebSocketConnection}/{@link WebSocketClientConnection}; depending on the endpoint type</li>
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
