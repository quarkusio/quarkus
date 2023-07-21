package io.quarkus.oidc;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which can be used to associate tenant configurations with Jakarta REST resources and resource methods.
 */
@Target({ TYPE, METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Tenant {
    /**
     * Identifies an OIDC tenant configurations.
     */
    String value();
}
