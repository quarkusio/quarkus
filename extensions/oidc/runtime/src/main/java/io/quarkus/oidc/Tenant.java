package io.quarkus.oidc;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which can be used to associate OIDC tenant configurations with the endpoint classes and methods.
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
