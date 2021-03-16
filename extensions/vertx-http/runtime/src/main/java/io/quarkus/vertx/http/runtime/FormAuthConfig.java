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
     * The post location.
     */
    @ConfigItem(defaultValue = "/j_security_check")
    public String postLocation;

    /**
     * The username field name.
     */
    @ConfigItem(defaultValue = "j_username")
    public String usernameParameter;

    /**
     * The password field name.
     */
    @ConfigItem(defaultValue = "j_password")
    public String passwordParameter;

    /**
     * The error page
     */
    @ConfigItem(defaultValue = "/error.html")
    public String errorPage;

    /**
     * The landing page to redirect to if there is no saved page to redirect back to
     */
    @ConfigItem(defaultValue = "/index.html")
    public String landingPage;

    /**
     * Option to disable redirect to landingPage if there is no saved page to redirect back to. Form Auth POST is followed
     * by redirect to landingPage by default.
     */
    @ConfigItem(defaultValue = "true")
    public boolean redirectAfterLogin;

    /**
     * Option to control the name of the cookie used to redirect the user back
     * to where he want to get access to.
     */
    @ConfigItem(defaultValue = "quarkus-redirect-location")
    public String locationCookie;

    /**
     * The inactivity (idle) timeout
     *
     * When inactivity timeout is reached, cookie is not renewed and a new login is enforced.
     */
    @ConfigItem(defaultValue = "PT30M")
    public Duration timeout;

    /**
     * How old a cookie can get before it will be replaced with a new cookie with an updated timeout, also
     * referred to as "renewal-timeout".
     *
     * Note that smaller values will result in slightly more server load (as new encrypted cookies will be
     * generated more often), however larger values affect the inactivity timeout as the timeout is set
     * when a cookie is generated.
     *
     * For example if this is set to 10 minutes, and the inactivity timeout is 30m, if a users last request
     * is when the cookie is 9m old then the actual timeout will happen 21m after the last request, as the timeout
     * is only refreshed when a new cookie is generated.
     *
     * In other words no timeout is tracked on the server side; the timestamp is encoded and encrypted in the cookie itself
     * and it is decrypted and parsed with each request.
     */
    @ConfigItem(defaultValue = "PT1M")
    public Duration newCookieInterval;

    /**
     * The cookie that is used to store the persistent session
     */
    @ConfigItem(defaultValue = "quarkus-credential")
    public String cookieName;
}
