package io.quarkus.oidc.client;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

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
}
