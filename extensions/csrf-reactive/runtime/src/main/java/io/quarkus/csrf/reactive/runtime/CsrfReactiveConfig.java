package io.quarkus.csrf.reactive.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Runtime configuration for CSRF Reactive Filter.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class CsrfReactiveConfig {
    /**
     * Form field name which keeps a CSRF token.
     */
    @ConfigItem(defaultValue = "csrf-token")
    public String formFieldName;

    /**
     * CSRF cookie name.
     */
    @ConfigItem(defaultValue = "csrf-token")
    public String cookieName;

    /**
     * CSRF cookie max age.
     */
    @ConfigItem(defaultValue = "10M")
    public Duration cookieMaxAge;

    /**
     * CSRF cookie path.
     */
    @ConfigItem(defaultValue = "/")
    public String cookiePath;

    /**
     * CSRF cookie domain.
     */
    @ConfigItem
    public Optional<String> cookieDomain;

    /**
     * If enabled the CSRF cookie will have its 'secure' parameter set to 'true'
     * when HTTP is used. It may be necessary when running behind an SSL terminating reverse proxy.
     * The cookie will always be secure if HTTPS is used even if this property is set to false.
     */
    @ConfigItem(defaultValue = "false")
    public boolean cookieForceSecure;

    /**
     * Create CSRF token only if the HTTP GET relative request path matches one of the paths configured with this property.
     * Use a comma to separate multiple path values.
     *
     */
    @ConfigItem
    public Optional<Set<String>> createTokenPath;

    /**
     * The random CSRF token size in bytes.
     */
    @ConfigItem(defaultValue = "16")
    public int tokenSize;

    /**
     * Verify CSRF token in the CSRF filter.
     * If this property is enabled then the input stream will be read and cached by the CSRF filter to verify the token.
     *
     * If you prefer then you can disable this property and compare
     * CSRF form and cookie parameters in the application code using JAX-RS jakarta.ws.rs.FormParam which refers to the
     * {@link #formFieldName}
     * form property and jakarta.ws.rs.CookieParam which refers to the {@link CsrfReactiveConfig#cookieName} cookie.
     *
     * Note that even if the CSRF token verification in the CSRF filter is disabled, the filter will still perform checks to
     * ensure the token
     * is available, has the correct {@linkplain #tokenSize} in bytes and that the Content-Type HTTP header is
     * either 'application/x-www-form-urlencoded' or 'multipart/form-data'.
     */
    @ConfigItem(defaultValue = "true")
    public boolean verifyToken;

    /**
     * Require that only 'application/x-www-form-urlencoded' or 'multipart/form-data' body is accepted for the token
     * verification to proceed.
     * Disable this property for the CSRF filter to avoid verifying the token for POST requests with other content types.
     * This property is only effective if {@link #verifyToken} property is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean requireFormUrlEncoded;
}
