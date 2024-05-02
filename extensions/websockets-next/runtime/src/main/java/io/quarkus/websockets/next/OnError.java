package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * {@link WebSocket} and {@link WebSocketClient} endpoint methods annotated with this annotation are invoked when an error
 * occurs.
 * <p>
 * It is used when an endpoint callback throws a runtime error, or when a conversion errors occurs, or when a returned
 * {@link io.smallrye.mutiny.Uni} receives a failure.
 * <p>
 * The method must accept exactly one "error" parameter, i.e. a parameter that is assignable from {@link java.lang.Throwable}.
 * The method may also accept the following parameters:
 * <ul>
 * <li>{@link WebSocketConnection}/{@link WebSocketClientConnection}; depending on the endpoint type</li>
 * <li>{@link HandshakeRequest}</li>
 * <li>{@link String} parameters annotated with {@link PathParam}</li>
 * </ul>
 * <p>
 * An endpoint may declare multiple methods annotated with this annotation. However, each method must declare a different error
 * parameter. The method that declares a most-specific supertype of the actual exception is selected.
 * <p>
 * This annotation can be also used to declare a global error handler, i.e. a method that is not declared on a
 * {@link WebSocket}/{@link WebSocketClient} endpoint. Such a method may not accept {@link PathParam} paremeters. If a global
 * error handler accepts {@link WebSocketConnection} then it's only applied to server-side errors. If a global error
 * handler accepts {@link WebSocketClientConnection} then it's only applied to client-side errors.
 *
 * Error handlers declared on an endpoint take precedence over the global error handlers.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Experimental("This API is experimental and may change in the future")
public @interface OnError {

}
