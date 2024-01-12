package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * config for the form authentication mechanism
 */
@ConfigGroup
public class FormAuthRuntimeConfig {
    /**
     * SameSite attribute values for the session and location cookies.
     */
    public enum CookieSameSite {
        STRICT,
        LAX,
        NONE
    }

    /**
     * The login page. Redirect to login page can be disabled by setting `quarkus.http.auth.form.login-page=`.
     */
    @ConfigItem(defaultValue = "/login.html")
    public Optional<String> loginPage;

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
     * The error page. Redirect to error page can be disabled by setting `quarkus.http.auth.form.error-page=`.
     */
    @ConfigItem(defaultValue = "/error.html")
    public Optional<String> errorPage;

    /**
     * The landing page to redirect to if there is no saved page to redirect back to.
     * Redirect to landing page can be disabled by setting `quarkus.http.auth.form.landing-page=`.
     */
    @ConfigItem(defaultValue = "/index.html")
    public Optional<String> landingPage;

    /**
     * Option to disable redirect to landingPage if there is no saved page to redirect back to. Form Auth POST is followed
     * by redirect to landingPage by default.
     *
     * @deprecated redirect to landingPage can be disabled by removing default landing page
     *             (via `quarkus.http.auth.form.landing-page=`). Quarkus will ignore this configuration property
     *             if there is no landing page.
     */
    @ConfigItem(defaultValue = "true")
    @Deprecated
    public boolean redirectAfterLogin;

    /**
     * Option to control the name of the cookie used to redirect the user back
     * to the location they want to access.
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
     * generated more often); however, larger values affect the inactivity timeout because the timeout is set
     * when a cookie is generated.
     *
     * For example if this is set to 10 minutes, and the inactivity timeout is 30m, if a user's last request
     * is when the cookie is 9m old then the actual timeout will happen 21m after the last request because the timeout
     * is only refreshed when a new cookie is generated.
     *
     * That is, no timeout is tracked on the server side; the timestamp is encoded and encrypted in the cookie
     * itself, and it is decrypted and parsed with each request.
     */
    @ConfigItem(defaultValue = "PT1M")
    public Duration newCookieInterval;

    /**
     * The cookie that is used to store the persistent session
     */
    @ConfigItem(defaultValue = "quarkus-credential")
    public String cookieName;

    /**
     * The cookie path for the session and location cookies.
     */
    @ConfigItem(defaultValue = "/")
    public Optional<String> cookiePath = Optional.of("/");

    /**
     * Set the HttpOnly attribute to prevent access to the cookie via JavaScript.
     */
    @ConfigItem(defaultValue = "false")
    public boolean httpOnlyCookie;

    /**
     * SameSite attribute for the session and location cookies.
     */
    @ConfigItem(defaultValue = "strict")
    public CookieSameSite cookieSameSite = CookieSameSite.STRICT;
}
