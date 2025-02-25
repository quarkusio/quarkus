package io.quarkus.csrf.reactive.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime configuration for CSRF Reactive Filter.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.rest-csrf")
public interface RestCsrfConfig {
    /**
     * Form field name which keeps a CSRF token.
     */
    @WithDefault("csrf-token")
    String formFieldName();

    /**
     * Token header which can provide a CSRF token.
     */
    @WithDefault("X-CSRF-TOKEN")
    String tokenHeaderName();

    /**
     * CSRF cookie name.
     */
    @WithDefault("csrf-token")
    String cookieName();

    /**
     * CSRF cookie max age.
     */
    @WithDefault("2H")
    Duration cookieMaxAge();

    /**
     * CSRF cookie path.
     */
    @WithDefault("/")
    String cookiePath();

    /**
     * CSRF cookie domain.
     */
    Optional<String> cookieDomain();

    /**
     * If enabled the CSRF cookie will have its 'secure' parameter set to 'true'
     * when HTTP is used. It may be necessary when running behind an SSL terminating reverse proxy.
     * The cookie will always be secure if HTTPS is used even if this property is set to false.
     */
    @WithDefault("false")
    boolean cookieForceSecure();

    /**
     * Set the HttpOnly attribute to prevent access to the cookie via JavaScript.
     */
    @WithDefault("true")
    boolean cookieHttpOnly();

    /**
     * Create CSRF token only if the HTTP GET relative request path matches one of the paths configured with this property.
     * Use a comma to separate multiple path values.
     *
     */
    Optional<Set<String>> createTokenPath();

    /**
     * Random CSRF token size in bytes.
     */
    @WithDefault("16")
    int tokenSize();

    /**
     * CSRF token HMAC signature key, if this key is set then it must be at least 32 characters long.
     */
    Optional<String> tokenSignatureKey();

    /**
     * Verify CSRF token in the CSRF filter.
     *
     * If you prefer then you can disable this property and compare
     * CSRF form and cookie parameters in the application code using JAX-RS jakarta.ws.rs.FormParam which refers to the
     * {@link #formFieldName}
     * form property and jakarta.ws.rs.CookieParam which refers to the {@link RestCsrfConfig#cookieName} cookie.
     *
     * Note that even if the CSRF token verification in the CSRF filter is disabled, the filter will still perform checks to
     * ensure the token
     * is available, has the correct {@linkplain #tokenSize} in bytes and that the Content-Type HTTP header is
     * either 'application/x-www-form-urlencoded' or 'multipart/form-data'.
     */
    @WithDefault("true")
    boolean verifyToken();

    /**
     * Require that only 'application/x-www-form-urlencoded' or 'multipart/form-data' body is accepted for the token
     * verification to proceed.
     * Disable this property for the CSRF filter to avoid verifying the token for POST requests with other content types.
     * This property is only effective if {@link #verifyToken} property is enabled and {@link #tokenHeaderName} is not
     * configured.
     */
    @WithDefault("true")
    boolean requireFormUrlEncoded();
}
