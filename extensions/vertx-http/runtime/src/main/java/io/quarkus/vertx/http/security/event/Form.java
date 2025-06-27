package io.quarkus.vertx.http.security.event;

import java.time.Duration;

import io.quarkus.vertx.http.runtime.FormAuthConfig;
import io.quarkus.vertx.http.runtime.FormAuthConfig.CookieSameSite;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;

/**
 * A CDI event that facilitates programmatic setup of the Form-based authentication.
 */
public interface Form {

    /**
     * Enables the Form-based authentication during the runtime if it wasn't already explicitly enabled with
     * the 'quarkus.http.auth.form.enabled' configuration property during the build-time.
     * If you require that the {@link io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism} can be injected
     * as a CDI bean, use the 'quarkus.http.auth.form.enabled' configuration property instead.
     *
     * @return Form
     */
    Form enable();

    /**
     * Configures the post location.
     *
     * @param postLocation see the 'quarkus.http.auth.form.post-location' configuration property
     * @return Form
     * @see FormAuthConfig#postLocation()
     */
    Form postLocation(String postLocation);

    /**
     * Configures the login page.
     *
     * @param loginPage see the 'quarkus.http.auth.form.login-page' configuration property
     * @return Form
     * @see FormAuthConfig#loginPage()
     */
    Form loginPage(String loginPage);

    /**
     * Configures the username field name.
     *
     * @param usernameParameter see the 'quarkus.http.auth.form.username-parameter' configuration property
     * @return Form
     * @see FormAuthConfig#usernameParameter()
     */
    Form usernameParameter(String usernameParameter);

    /**
     * Configures the password field name.
     *
     * @param passwordParameter see the 'quarkus.http.auth.form.password-parameter' configuration property
     * @return Form
     * @see FormAuthConfig#passwordParameter()
     */
    Form passwordParameter(String passwordParameter);

    /**
     * Configures the error page.
     *
     * @param errorPage see the 'quarkus.http.auth.form.error-page' configuration property
     * @return Form
     * @see FormAuthConfig#errorPage()
     */
    Form errorPage(String errorPage);

    /**
     * Configures the landing page to redirect to if there is no saved page to redirect back to.
     *
     * @param landingPage see the 'quarkus.http.auth.form.landing-page' configuration property
     * @return Form
     * @see FormAuthConfig#landingPage()
     */
    Form landingPage(String landingPage);

    /**
     * Configures a name for the cookie that is used to redirect the user back to the location they want to access.
     *
     * @param locationCookie see the 'quarkus.http.auth.form.location-cookie' configuration property
     * @return Form
     * @see FormAuthConfig#locationCookie()
     */
    Form locationCookie(String locationCookie);

    /**
     * Configures the inactivity timeout.
     *
     * @param timeout see the 'quarkus.http.auth.form.timeout' configuration property
     * @return Form
     * @see FormAuthConfig#timeout()
     */
    Form timeout(Duration timeout);

    /**
     * Configures how old a cookie can get before it will be replaced with a new cookie with an updated timeout.
     *
     * @param newCookieInterval see the 'quarkus.http.auth.form.new-cookie-interval' configuration property
     * @return Form
     * @see FormAuthConfig#newCookieInterval()
     */
    Form newCookieInterval(Duration newCookieInterval);

    /**
     * Configures a name for the cookie that is used to store the persistent session.
     *
     * @param cookieName see the 'quarkus.http.auth.form.cookie-name' configuration property
     * @return Form
     * @see FormAuthConfig#cookieName()
     */
    Form cookieName(String cookieName);

    /**
     * Configures the cookie path for the session and location cookies.
     *
     * @param cookiePath see the 'quarkus.http.auth.form.cookie-path' configuration property
     * @return Form
     * @see FormAuthConfig#cookiePath()
     */
    Form cookiePath(String cookiePath);

    /**
     * Configures the 'domain' attribute for the session and location cookies.
     *
     * @param cookieDomain see the 'quarkus.http.auth.form.cookie-domain' configuration property
     * @return Form
     * @see FormAuthConfig#cookieDomain()
     */
    Form cookieDomain(String cookieDomain);

    /**
     * Configures the HttpOnly attribute to prevent access to the cookie via JavaScript.
     *
     * @param httpOnlyCookie see the 'quarkus.http.auth.form.http-only-cookie' configuration property
     * @return Form
     * @see FormAuthConfig#httpOnlyCookie()
     */
    Form httpOnlyCookie(boolean httpOnlyCookie);

    /**
     * This method is a shortcut for {@code httpOnlyCookie(true)}.
     *
     * @return Form
     * @see #httpOnlyCookie(boolean)
     */
    Form httpOnlyCookie();

    /**
     * Configures the SameSite attribute for the session and location cookies.
     *
     * @param cookieSameSite see the 'quarkus.http.auth.form.cookie-same-site' configuration property
     * @return Form
     * @see FormAuthConfig#cookieSameSite()
     */
    Form cookieSameSite(CookieSameSite cookieSameSite);

    /**
     * Configures the Max-Age attribute for the session cookie.
     *
     * @param cookieMaxAge see the 'quarkus.http.auth.form.cookie-max-age' configuration property
     * @return Form
     * @see FormAuthConfig#cookieMaxAge()
     */
    Form cookieMaxAge(Duration cookieMaxAge);

    /**
     * Configures the encryption key that is used to store persistent logins for the Form-based authentication.
     *
     * @param encryptionKey see the 'quarkus.http.auth.session.encryption-key' configuration property
     * @return Form
     * @see VertxHttpConfig#encryptionKey()
     */
    Form encryptionKey(String encryptionKey);
}
