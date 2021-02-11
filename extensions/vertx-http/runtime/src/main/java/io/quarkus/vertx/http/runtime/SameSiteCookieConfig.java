package io.quarkus.vertx.http.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.vertx.core.http.CookieSameSite;

/**
 * Configuration that allows for automatically setting the SameSite attribute on cookies
 *
 * As some API's (Servlet, JAX-RS) don't current support this attribute this config allows
 * it to be set based on the cookie name pattern.
 */
@ConfigGroup
public class SameSiteCookieConfig {

    /**
     * If the cookie pattern is case sensitive
     */
    @ConfigItem
    public boolean caseSensitive;

    /**
     * The value to set in the samesite attribute
     */
    @ConfigItem
    public CookieSameSite value;

    /**
     * Some User Agents break when sent SameSite=None, this will detect them and avoid sending the value
     */
    @ConfigItem(defaultValue = "true")
    public boolean enableClientChecker;

    /**
     * If this is true then the 'secure' attribute will automatically be sent on
     * cookies with a SameSite attribute of None.
     */
    @ConfigItem(defaultValue = "true")
    public boolean addSecureForNone;
}
