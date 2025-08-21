package io.quarkus.vertx.http.security;

import java.time.Duration;
import java.util.Set;

import io.smallrye.common.annotation.Experimental;

/**
 * This class provides a way to configure the Cross-Site Request Forgery (CSRF) prevention.
 */
@Experimental("This API is currently experimental and might get changed")
public interface CSRF {

    /**
     * Creates the CSRF prevention configuration builder.
     *
     * @return new {@link CSRF.Builder} instance
     */
    static Builder builder() {
        // when the CSRF capability is present, this method is transformed during the build time and returns a builder
        throw new IllegalStateException("Please add an extension that provides a CSRF prevention feature, for example "
                + "the Quarkus REST Cross-Site Request Forgery Prevention `quarkus-rest-csrf` extension");
    }

    /**
     * The Quarkus CSRF prevention configuration builder.
     */
    interface Builder {

        /**
         * Form field name which keeps a CSRF token. The default field name is "csrf-token".
         *
         * @param formFieldName form field name
         * @return this builder
         */
        Builder formFieldName(String formFieldName);

        /**
         * The token header name which can provide a CSRF token. The default name is "X-CSRF-TOKEN".
         *
         * @param tokenHeaderName the CSRF token header name
         * @return this builder
         */
        Builder tokenHeaderName(String tokenHeaderName);

        /**
         * The CSRF cookie name. The default name is "csrf-token".
         *
         * @param cookieName the CSRF cookie name
         * @return this builder
         */
        Builder cookieName(String cookieName);

        /**
         * The CSRF cookie max age. The default max age are 2 hours.
         *
         * @param cookieMaxAge the CSRF cookie max age
         * @return this builder
         */
        Builder cookieMaxAge(Duration cookieMaxAge);

        /**
         * The CSRF cookie path. The default path is "/".
         *
         * @param cookiePath the CSRF cookie path
         * @return this builder
         */
        Builder cookiePath(String cookiePath);

        /**
         * The CSRF cookie domain.
         *
         * @param cookieDomain the CSRF cookie domain
         * @return this builder
         */
        Builder cookieDomain(String cookieDomain);

        /**
         * Set the 'secure' parameter on the CSRF cookie to 'true' when the HTTP protocol is used.
         * The cookie will always be secure if the HTTPS protocol is used, even if this method is not called.
         *
         * @return this builder
         */
        Builder cookieForceSecure();

        /**
         * Set the HttpOnly attribute to prevent access to the cookie via JavaScript.
         * The HttpOnly attribute is set by default.
         *
         * @param cookieHttpOnly if the HttpOnly attribute should be set
         * @return this builder
         */
        Builder cookieHttpOnly(boolean cookieHttpOnly);

        /**
         * This method is a shortcut for {@code createTokenPath(Set.of(createTokenPath))}.
         *
         * @return this builder
         * @see #createTokenPath(Set) for more information
         */
        Builder createTokenPath(String createTokenPath);

        /**
         * Create CSRF token only if the HTTP GET relative request path matches one of configured paths.
         *
         * @param createTokenPath list of the HTTP GET requests paths for which Quarkus should create a token
         * @return this builder
         */
        Builder createTokenPath(Set<String> createTokenPath);

        /**
         * Random CSRF token size in bytes. The default size in bytes is 16.
         *
         * @param tokenSize the token size in bytes
         * @return this builder
         */
        Builder tokenSize(int tokenSize);

        /**
         * The CSRF token signature key.
         *
         * @param tokenSignatureKey the CSRF token signature key
         * @return this builder
         */
        Builder tokenSignatureKey(String tokenSignatureKey);

        /**
         * Require that only 'application/x-www-form-urlencoded' or 'multipart/form-data' body is accepted for the token
         * verification to proceed. Required by default.
         *
         * @param requireFormUrlEncoded if only 'application/x-www-form-urlencoded' or 'multipart/form-data' body is allowed
         * @return this builder
         */
        Builder requireFormUrlEncoded(boolean requireFormUrlEncoded);

        /**
         * Create a new CSRF configuration.
         *
         * @return CSRF instance, which should be passed to the {@link HttpSecurity} event
         */
        CSRF build();
    }

}
