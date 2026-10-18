package io.quarkus.aesh.runtime;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * CDI qualifier for events fired when an aesh remote session is closed.
 *
 * @see AeshSessionEvent
 */
@Qualifier
@Documented
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE })
public @interface SessionClosed {

    /**
     * Supports inline instantiation of the {@link SessionClosed} qualifier.
     */
    final class Literal extends AnnotationLiteral<SessionClosed> implements SessionClosed {

        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;
    }
}
