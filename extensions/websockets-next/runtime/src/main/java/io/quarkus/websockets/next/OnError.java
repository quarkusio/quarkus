package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * {@link WebSocket} and {@link WebSocketClient} endpoint methods annotated with this annotation are invoked when an error
 * occurs.
 * <p>
 * It is used when an endpoint callback throws a runtime error, or when a conversion errors occurs, or when a returned
 * {@link io.smallrye.mutiny.Uni} receives a failure. An endpoint may declare multiple methods annotated with this annotation.
 * However, each method must declare a different error
 * parameter. The method that declares a most-specific supertype of the actual exception is selected.
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
 * The method must accept exactly one "error" parameter, i.e. a parameter that is assignable from {@link java.lang.Throwable}.
 * The method may also accept the following parameters:
 * <ul>
 * <li>{@link WebSocketConnection}/{@link WebSocketClientConnection}; depending on the endpoint type</li>
 * <li>{@link HandshakeRequest}</li>
 * <li>{@link String} parameters annotated with {@link PathParam}</li>
 * </ul>
 * <p>
 *
 * <h2>Global error handlers</h2>
 *
 * This annotation can be also used to declare a global error handler, i.e. a method that is not declared on a
 * {@link WebSocket}/{@link WebSocketClient} endpoint. Such a method may not accept {@link PathParam} paremeters. If a global
 * error handler accepts {@link WebSocketConnection} then it's only applied to server-side errors. If a global error
 * handler accepts {@link WebSocketClientConnection} then it's only applied to client-side errors.
 *
 * Error handlers declared on an endpoint take precedence over the global error handlers.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface OnError {

}
