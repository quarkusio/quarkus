package io.quarkus.oidc;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation used to specify which named tenants are associated with an OIDC feature.
 */
@Target({ TYPE })
@Retention(RUNTIME)
@Documented
public @interface TenantFeature {
    /**
     * Identifies one or more OIDC tenants to which a given feature applies.
     */
    String[] value();
}
