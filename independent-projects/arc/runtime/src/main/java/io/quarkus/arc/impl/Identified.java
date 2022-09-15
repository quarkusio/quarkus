package io.quarkus.arc.impl;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Qualifies a bean with a string-based identifier.
 * <p>
 * This is an internal qualifier and should not be used by application beans.
 */
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, FIELD, METHOD, PARAMETER })
public @interface Identified {

    String value();

    /**
     * Supports inline instantiation of this qualifier.
     */
    public static final class Literal extends AnnotationLiteral<Identified> implements Identified {

        private static final long serialVersionUID = 1L;

        private final String value;

        public Literal(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

    }

}
