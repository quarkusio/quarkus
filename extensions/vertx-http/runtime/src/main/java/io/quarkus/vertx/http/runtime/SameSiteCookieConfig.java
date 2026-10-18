package io.quarkus.vertx.http.runtime;

import io.smallrye.config.WithDefault;
import io.vertx.core.http.CookieSameSite;

/**
 * Configuration that allows for automatically setting the SameSite attribute on cookies
 * <p>
 * As some API's (Servlet, JAX-RS) don't current support this attribute this config allows
 * it to be set based on the cookie name pattern.
 */
public interface SameSiteCookieConfig {
    /**
     * If the cookie pattern is case-sensitive
     */
    @WithDefault("false")
    boolean caseSensitive();

    /**
     * The value to set in the samesite attribute
     */
    CookieSameSite value();

    /**
     * Some User Agents break when sent SameSite=None, this will detect them and avoid sending the value
     */
    @WithDefault("true")
    boolean enableClientChecker();

    /**
     * If this is true then the 'secure' attribute will automatically be sent on
     * cookies with a SameSite attribute of None.
     */
    @WithDefault("true")
    boolean addSecureForNone();
}
