package io.quarkus.oidc;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to restrict {@link OidcRedirectFilter} to specific redirect locations
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface Redirect {

    enum Location {
        ALL,

        /**
         * Applies to OIDC authorization endpoint
         */
        OIDC_AUTHORIZATION,

        /**
         * Applies to OIDC logout endpoint
         */
        OIDC_LOGOUT,

        /**
         * Applies to the local redirect to a custom error page resource when an authorization code flow
         * redirect from OIDC provider to Quarkus returns an error instead of an authorization code
         */
        ERROR_PAGE,

        /**
         * Applies to the local redirect to a custom session expired page resource when
         * the current user's session has expired and no longer can be refreshed.
         */
        SESSION_EXPIRED_PAGE,

        /**
         * Applies to the local redirect to the callback resource which is done after successful authorization
         * code flow completion in order to drop the code and state parameters from the callback URL.
         */
        LOCAL_ENDPOINT_CALLBACK
    }

    /**
     * Identifies one or more redirect locations.
     */
    Location[] value() default Location.ALL;
}
