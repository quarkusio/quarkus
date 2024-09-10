package io.quarkus.oidc.common;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to restrict {@link OidcRequestFilter} to specific OIDC endpoints
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface OidcEndpoint {

    enum Type {
        ALL,

        /**
         * Applies to OIDC discovery requests
         */
        DISCOVERY,

        /**
         * Applies to OIDC token endpoint requests
         */
        TOKEN,

        /**
         * Applies to OIDC token revocation endpoint requests
         */
        TOKEN_REVOCATION,

        /**
         * Applies to OIDC token introspection requests
         */
        INTROSPECTION,
        /**
         * Applies to OIDC JSON Web Key Set endpoint requests
         */
        JWKS,
        /**
         * Applies to OIDC UserInfo endpoint requests
         */
        USERINFO,
        /**
         * Applies to OIDC client registration requests
         */
        CLIENT_REGISTRATION,
        /**
         * Applies to requests to dynamically registered OIDC client endpoints
         */
        REGISTERED_CLIENT
    }

    /**
     * Identifies one or more OIDC endpoints.
     */
    Type[] value() default Type.ALL;
}
