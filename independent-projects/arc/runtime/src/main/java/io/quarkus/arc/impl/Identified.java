package io.quarkus.arc.impl;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Qualifies a bean with a string-based identifier.
 * <p>
 * This is an internal qualifier and should not be used by application beans.
 *
 * @deprecated This is an internal qualifier and should have never been used publicly.
 *             It shall be removed at some point after Quarkus 3.27. See also
 *             <a href="https://github.com/quarkusio/quarkus/pull/32179">#32179</a> and
 *             <a href="https://github.com/quarkusio/quarkus/pull/49178">#49178</a>.
 */
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, FIELD, METHOD, PARAMETER })
@Deprecated(forRemoval = true, since = "3.26")
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
