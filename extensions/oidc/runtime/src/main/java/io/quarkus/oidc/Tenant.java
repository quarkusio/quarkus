package io.quarkus.oidc;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * Annotation which can be used to associate one or more OIDC features with a named tenant.
 */
@Target({ TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Experimental("Tenant annnotation is experimental and may change without notice")
public @interface Tenant {
    /**
     * Identifies an OIDC tenant to which a given feature applies.
     */
    String value();
}
