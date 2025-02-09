package io.quarkus.oidc;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which can be used to associate OIDC tenant configurations with Jakarta REST resources and resource methods.
 * It is also possible to use the annotation to associate OIDC tenant configuration with WebSockets Next endpoints.
 * When placed on injection points, this annotation can be used to select a tenant associated
 * with the {@link TenantIdentityProvider}.
 */
@Target({ TYPE, METHOD, FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface Tenant {
    /**
     * Identifies an OIDC tenant configurations.
     */
    String value();
}
