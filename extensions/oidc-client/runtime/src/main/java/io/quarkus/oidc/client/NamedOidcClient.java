package io.quarkus.oidc.client;

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
 * Specification of OIDC client to be injected.
 */
@Target({ TYPE, FIELD, METHOD, PARAMETER })
@Retention(RUNTIME)
@Qualifier
@Documented
public @interface NamedOidcClient {
    /**
     * @return name of OIDC client to be injected
     */
    String value();

    public static final class Literal extends AnnotationLiteral<NamedOidcClient> implements NamedOidcClient {

        private static final long serialVersionUID = 1L;
        private final String value;

        public static Literal of(String value) {
            return new Literal(value);
        }

        private Literal(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return this.value;
        }
    }
}
