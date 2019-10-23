package io.quarkus.vertx.http.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * config for the form authentication mechanism
 */
@ConfigGroup
public class FormAuthConfig {
    /**
     * If form authentication is enabled
     */
    @ConfigItem
    public boolean enabled;

    /**
     * The login page
     */
    @ConfigItem(defaultValue = "/login.html")
    public String loginPage;

    /**
     * The error page
     */
    @ConfigItem(defaultValue = "/error.html")
    public String errorPage;

    /**
     * The landing page to redirect to if there is no saved page to redirect back to
     */
    @ConfigItem
    public String landingPage;

    /**
     * The inactivity timeout
     */
    @ConfigItem(defaultValue = "PT30M")
    public Duration timeout;

    /**
     * The cookie that is used to store the persistent session
     */
    @ConfigItem(defaultValue = "quarkus-credential")
    public String cookieName;
}
