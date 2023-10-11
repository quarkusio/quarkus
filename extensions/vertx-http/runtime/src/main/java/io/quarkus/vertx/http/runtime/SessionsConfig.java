package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.vertx.core.http.CookieSameSite;

/**
 * Configuration of Vert.x Web sessions.
 */
@ConfigGroup
public class SessionsConfig {
    /**
     * The session timeout.
     */
    @ConfigItem(defaultValue = "30M")
    public Duration timeout;

    /**
     * The requested length of the session identifier.
     */
    @ConfigItem(defaultValue = "16")
    public int idLength;

    /**
     * The session cookie path. The value is relative to {@code quarkus.http.root-path}.
     */
    @ConfigItem(defaultValue = "/")
    public String path;

    /**
     * The name of the session cookie.
     */
    @ConfigItem(defaultValue = "JSESSIONID")
    public String cookieName;

    /**
     * Whether the session cookie has the {@code HttpOnly} attribute.
     */
    @ConfigItem(defaultValue = "true")
    public boolean cookieHttpOnly;

    /**
     * Whether the session cookie has the {@code Secure} attribute.
     * <ul>
     * <li>{@code always}: the session cookie always has the {@code Secure} attribute</li>
     * <li>{@code never}: the session cookie never has the {@code Secure} attribute</li>
     * <li>{@code auto}: the session cookie only has the {@code Secure} attribute when {@code quarkus.http.insecure-requests}
     * is {@code redirect} or {@code disabled}; if {@code insecure-requests} is {@code enabled}, the session cookie
     * does not have the {@code Secure} attribute
     * </ul>
     */
    @ConfigItem(defaultValue = "auto")
    public SessionCookieSecure cookieSecure;

    /**
     * The value of the {@code SameSite} attribute of the session cookie.
     * By default, the {@code SameSite} attribute is not present.
     */
    @ConfigItem
    public Optional<CookieSameSite> cookieSameSite;

    /**
     * The {@code Max-Age} attribute of the session cookie. Note that setting this option turns the session cookie
     * into a <em>persistent</em> cookie.
     */
    @ConfigItem
    public Optional<Duration> cookieMaxAge;

    public enum SessionCookieSecure {
        /**
         * The session cookie only has the {@code Secure} attribute when {@code quarkus.http.insecure-requests}
         * is {@code redirect} or {@code disabled}. If {@code insecure-requests} is {@code enabled}, the session cookie
         * does not have the {@code Secure} attribute.
         */
        AUTO,
        /**
         * The session cookie always has the {@code Secure} attribute.
         */
        ALWAYS,
        /**
         * The session cookie never has the {@code Secure} attribute.
         */
        NEVER;

        boolean isEnabled(HttpConfiguration.InsecureRequests insecureRequests) {
            if (this == ALWAYS) {
                return true;
            } else if (this == NEVER) {
                return false;
            } else {
                return insecureRequests != HttpConfiguration.InsecureRequests.ENABLED;
            }
        }
    }
}
