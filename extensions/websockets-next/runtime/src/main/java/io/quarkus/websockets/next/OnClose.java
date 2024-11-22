package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * {@link WebSocket} and {@link WebSocketClient} endpoint methods annotated with this annotation are invoked when a connection
 * is closed. An endpoint may declare at most one method annotated with this annotation. The method must return {@code void} or
 * {@code io.smallrye.mutiny.Uni<Void>}.
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
 * <li>Methods returning {@code io.smallrye.mutiny.Uni<Void>} are considered non-blocking and should be executed on an event
 * loop thread.</li>
 * </ul>
 *
 * <h2>Method parameters</h2>
 *
 * The method may accept the following parameters:
 * <ul>
 * <li>{@link WebSocketConnection}/{@link WebSocketClientConnection}; depending on the endpoint type</li>
 * <li>{@link HandshakeRequest}</li>
 * <li>{@link String} parameters annotated with {@link PathParam}</li>
 * <li>{@link CloseReason}</li>
 * </ul>
 * Note that it's not possible to send a message to the current connection as the socket is already closed when the method
 * invoked. However, it is possible to send messages to other open connections.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface OnClose {

}
