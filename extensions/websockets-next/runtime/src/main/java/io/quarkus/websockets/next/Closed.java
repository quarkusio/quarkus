package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * This qualifier is used for CDI events fired asynchronously when a WebSocket connection is closed.
 * <p>
 * The payload is {@link WebSocketConnection} for server connections and {@link WebSocketClientConnection} for client
 * connections.
 *
 * @see ObservesAsync
 * @see Event#fireAsync(Object)
 */
@Qualifier
@Documented
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE })
public @interface Closed {

    /**
     * Supports inline instantiation of the {@link Closed} qualifier.
     */
    public static final class Literal extends AnnotationLiteral<Closed> implements Closed {

        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;

    }

}
