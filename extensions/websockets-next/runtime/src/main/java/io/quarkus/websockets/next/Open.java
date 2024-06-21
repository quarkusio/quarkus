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
 * This qualifier is used for CDI events fired asynchronously when a new WebSocket connection is opened.
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
public @interface Open {

    /**
     * Supports inline instantiation of the {@link Open} qualifier.
     */
    public static final class Literal extends AnnotationLiteral<Open> implements Open {

        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;

    }

}
