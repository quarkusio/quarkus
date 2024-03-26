package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.quarkus.websockets.next.WebSocketConnection.HandshakeRequest;
import io.smallrye.common.annotation.Experimental;

/**
 * A {@link WebSocket} endpoint method annotated with this annotation is invoked when an error occurs.
 * <p>
 * The method must accept exactly one "error" parameter, i.e. a parameter that is assignable from {@link java.lang.Throwable}.
 * The method may also accept the following parameters:
 * <ul>
 * <li>{@link WebSocketConnection}</li>
 * <li>{@link HandshakeRequest}</li>
 * <li>{@link String} parameters annotated with {@link PathParam}</li>
 * </ul>
 * <p>
 * An endpoint may declare multiple methods annotated with this annotation. However, each method must declare a unique error
 * parameter. The method that declares a most-specific supertype of the actual exception is selected.
 * <p>
 * This annotation can be also used to declare a global error handler, i.e. a method that is not declared on a {@link WebSocket}
 * endpoint. Such a method may not not accept {@link PathParam} paremeters. Error handlers declared on an endpoint take
 * precedence over the global error handlers.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Experimental("This API is experimental and may change in the future")
public @interface OnError {

}
