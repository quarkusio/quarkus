package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.Optional;

import io.smallrye.config.WithDefault;

/**
 * config for the form authentication mechanism
 */
public interface FormAuthRuntimeConfig {
    /**
     * SameSite attribute values for the session and location cookies.
     */
    enum CookieSameSite {
        STRICT,
        LAX,
        NONE
    }

    /**
     * The login page. Redirect to login page can be disabled by setting `quarkus.http.auth.form.login-page=`.
     */
    @WithDefault("/login.html")
    Optional<String> loginPage();

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
    @Deprecated(forRemoval = true, since = "2.16")
    boolean redirectAfterLogin();

    /**
     * Option to control the name of the cookie used to redirect the user back
     * to the location they want to access.
     */
    @WithDefault("quarkus-redirect-location")
    String locationCookie();

    /**
     * The inactivity (idle) timeout
     * <p>
     * When inactivity timeout is reached, cookie is not renewed and a new login is enforced.
     */
    @WithDefault("PT30M")
    Duration timeout();

    /**
     * How old a cookie can get before it will be replaced with a new cookie with an updated timeout, also
     * referred to as "renewal-timeout".
     * <p>
     * Note that smaller values will result in slightly more server load (as new encrypted cookies will be
     * generated more often); however, larger values affect the inactivity timeout because the timeout is set
     * when a cookie is generated.
     * <p>
     * For example if this is set to 10 minutes, and the inactivity timeout is 30m, if a user's last request
     * is when the cookie is 9m old then the actual timeout will happen 21m after the last request because the timeout
     * is only refreshed when a new cookie is generated.
     * <p>
     * That is, no timeout is tracked on the server side; the timestamp is encoded and encrypted in the cookie
     * itself, and it is decrypted and parsed with each request.
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

    /**
     * Max-Age attribute for the session cookie. This is the amount of time the browser will keep the cookie.
     * <p>
     * The default value is empty, which means the cookie will be kept until the browser is closed.
     */
    Optional<Duration> cookieMaxAge();

    /**
     * The post location.
     */
    @WithDefault("/j_security_check")
    String postLocation();

    /**
     * One-time authentication token configuration.
     */
    AuthenticationToken authenticationToken();

    /**
     * One-time authentication token configuration.
     */
    interface AuthenticationToken {

        /**
         * Enables one-time authentication token configuration as a single-factor authentication by allowing to request
         * the one-time authentication token without providing valid username and password.
         * By default, the one-time authentication token serves as the second-factor of the two-factor authentication,
         * and the authentication token is generated only if user sent valid username and password to the post location.
         */
        Optional<String> requestPath();

        /**
         * Quarkus will redirect to this path once one-time authentication token has been successfully generated.
         * When this configuration property is not set, Quarkus will respond to successful
         * one-time authentication token requests with the HTTP status `204 No Content`.
         */
        @WithDefault("/authentication-token.html")
        Optional<String> requestRedirectPath();

        /**
         * The one-time authentication token form field name.
         */
        @WithDefault("j_token")
        String formParameterName();

        /**
         * The one-time authentication token expiration time.
         * By default, the one-time authentication token expires in 5 minutes.
         */
        @WithDefault("PT5M")
        Duration expiresIn();
    }
}
