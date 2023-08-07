package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.Optional;

import io.smallrye.config.WithDefault;

/**
 * config for the form authentication mechanism
 */
public interface FormAuthConfig {
    /**
     * SameSite attribute values for the session and location cookies.
     */
    enum CookieSameSite {
        STRICT,
        LAX,
        NONE
    }

    /**
     * If form authentication is enabled.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * The login page. Redirect to login page can be disabled by setting `quarkus.http.auth.form.login-page=`.
     */
    @WithDefault("/login.html")
    Optional<String> loginPage();

    /**
     * The post location.
     */
    @WithDefault("/j_security_check")
    String postLocation();

    /**
     * The username field name.
     */
    @WithDefault("j_username")
    String usernameParameter();

    /**
     * The password field name.
     */
    @WithDefault("j_password")
    String passwordParameter();

    /**
     * The error page. Redirect to error page can be disabled by setting `quarkus.http.auth.form.error-page=`.
     */
    @WithDefault("/error.html")
    Optional<String> errorPage();

    /**
     * The landing page to redirect to if there is no saved page to redirect back to.
     * Redirect to landing page can be disabled by setting `quarkus.http.auth.form.landing-page=`.
     */
    @WithDefault("/index.html")
    Optional<String> landingPage();

    /**
     * Option to disable redirect to landingPage if there is no saved page to redirect back to. Form Auth POST is followed
     * by redirect to landingPage by default.
     *
     * @deprecated redirect to landingPage can be disabled by removing default landing page
     *             (via `quarkus.http.auth.form.landing-page=`). Quarkus will ignore this configuration property
     *             if there is no landing page.
     */
    @WithDefault("true")
    @Deprecated
    boolean redirectAfterLogin();

    /**
     * Option to control the name of the cookie used to redirect the user back
     * to where he wants to get access to.
     */
    @WithDefault("quarkus-redirect-location")
    String locationCookie();

    /**
     * The inactivity (idle) timeout
     *
     * When inactivity timeout is reached, cookie is not renewed and a new login is enforced.
     */
    @WithDefault("PT30M")
    Duration timeout();

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
     * In other words no timeout is tracked on the server side; the timestamp is encoded and encrypted in the cookie itself,
     * and it is decrypted and parsed with each request.
     */
    @WithDefault("PT1M")
    Duration newCookieInterval();

    /**
     * The cookie that is used to store the persistent session
     */
    @WithDefault("quarkus-credential")
    String cookieName();

    /**
     * The cookie path for the session and location cookies.
     */
    @WithDefault("/")
    Optional<String> cookiePath();

    /**
     * Set the HttpOnly attribute to prevent access to the cookie via JavaScript.
     */
    @WithDefault("false")
    boolean httpOnlyCookie();

    /**
     * SameSite attribute for the session and location cookies.
     */
    @WithDefault("strict")
    CookieSameSite cookieSameSite();
}
