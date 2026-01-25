package io.quarkus.vertx.http.security;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.vertx.http.runtime.FormAuthConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.common.annotation.Experimental;
import io.smallrye.config.SmallRyeConfig;

/**
 * This class provides a way to create a Form-based authentication mechanism. The {@link HttpAuthenticationMechanism}
 * created with this class can be registered using the {@link HttpSecurity#mechanism(HttpAuthenticationMechanism)} method.
 */
@Experimental("This API is currently experimental and might get changed")
public interface Form {

    /**
     * @return Form-based authentication configuration builder populated with a configuration provided
     *         in the 'application.properties' file. If no configuration was provided, the builder is populated with
     *         configuration defaults.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new Form-based authentication mechanism with a configuration provided in the 'application.properties'
     * file. If no configuration was provided, the default values are used instead.
     *
     * @return HttpAuthenticationMechanism
     */
    static HttpAuthenticationMechanism create() {
        return builder().build();
    }

    final class Builder {

        private String postLocation;
        private Optional<String> loginPage;
        private String usernameParameter;
        private String passwordParameter;
        private Optional<String> errorPage;
        private Optional<String> landingPage;
        private String locationCookie;
        private Duration timeout;
        private Duration newCookieInterval;
        private String cookieName;
        private Optional<String> cookiePath;
        private Optional<String> cookieDomain;
        private boolean httpOnlyCookie;
        private FormAuthConfig.CookieSameSite cookieSameSite;
        private Optional<Duration> cookieMaxAge;
        private Optional<String> encryptionKey;
        private Optional<Set<String>> landingPageQueryParams;
        private Optional<Set<String>> errorPageQueryParams;
        private Optional<Set<String>> loginPageQueryParams;
        private int priority;

        public Builder() {
            this(ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getConfigMapping(VertxHttpConfig.class));
        }

        private Builder(VertxHttpConfig vertxHttpConfig) {
            FormAuthConfig formAuthConfig = vertxHttpConfig.auth().form();
            this.postLocation = formAuthConfig.postLocation();
            this.loginPage = formAuthConfig.loginPage();
            this.usernameParameter = formAuthConfig.usernameParameter();
            this.passwordParameter = formAuthConfig.passwordParameter();
            this.errorPage = formAuthConfig.errorPage();
            this.landingPage = formAuthConfig.landingPage();
            this.locationCookie = formAuthConfig.locationCookie();
            this.timeout = formAuthConfig.timeout();
            this.newCookieInterval = formAuthConfig.newCookieInterval();
            this.cookieName = formAuthConfig.cookieName();
            this.cookiePath = formAuthConfig.cookiePath();
            this.cookieDomain = formAuthConfig.cookieDomain();
            this.httpOnlyCookie = formAuthConfig.httpOnlyCookie();
            this.cookieSameSite = formAuthConfig.cookieSameSite();
            this.cookieMaxAge = formAuthConfig.cookieMaxAge();
            this.encryptionKey = vertxHttpConfig.encryptionKey();
            this.landingPageQueryParams = formAuthConfig.landingPageQueryParams();
            this.errorPageQueryParams = formAuthConfig.errorPageQueryParams();
            this.loginPageQueryParams = formAuthConfig.loginPageQueryParams();
            this.priority = formAuthConfig.priority();
        }

        /**
         * Configures the post location.
         *
         * @param postLocation see the 'quarkus.http.auth.form.post-location' configuration property
         * @return Builder
         * @see FormAuthConfig#postLocation()
         */
        public Builder postLocation(String postLocation) {
            Objects.requireNonNull(postLocation);
            this.postLocation = postLocation;
            return this;
        }

        /**
         * Configures the login page.
         *
         * @param loginPage see the 'quarkus.http.auth.form.login-page' configuration property
         * @return Builder
         * @see FormAuthConfig#loginPage()
         */
        public Builder loginPage(String loginPage) {
            this.loginPage = Optional.ofNullable(loginPage);
            return this;
        }

        /**
         * Configures query parameters Quarkus passes through when redirecting requests to the login page.
         *
         * @param queryParameters query parameters; must not be null
         * @return Builder
         * @see FormAuthConfig#loginPageQueryParams()
         */
        public Builder loginPageQueryParameters(String... queryParameters) {
            if (queryParameters != null) {
                this.loginPageQueryParams = Optional.of(Set.of(queryParameters));
            }
            return this;
        }

        /**
         * Configures the username field name.
         *
         * @param usernameParameter see the 'quarkus.http.auth.form.username-parameter' configuration property
         * @return Builder
         * @see FormAuthConfig#usernameParameter()
         */
        public Builder usernameParameter(String usernameParameter) {
            Objects.requireNonNull(usernameParameter);
            this.usernameParameter = usernameParameter;
            return this;
        }

        /**
         * Configures the password field name.
         *
         * @param passwordParameter see the 'quarkus.http.auth.form.password-parameter' configuration property
         * @return Builder
         * @see FormAuthConfig#passwordParameter()
         */
        public Builder passwordParameter(String passwordParameter) {
            Objects.requireNonNull(passwordParameter);
            this.passwordParameter = passwordParameter;
            return this;
        }

        /**
         * Configures the error page.
         *
         * @param errorPage see the 'quarkus.http.auth.form.error-page' configuration property
         * @return Builder
         * @see FormAuthConfig#errorPage()
         */
        public Builder errorPage(String errorPage) {
            this.errorPage = Optional.ofNullable(errorPage);
            return this;
        }

        /**
         * Configures query parameters Quarkus passes through when redirecting requests to the error page.
         *
         * @param queryParameters query parameters; must not be null
         * @return Builder
         * @see FormAuthConfig#errorPageQueryParams()
         */
        public Builder errorPageQueryParameters(String... queryParameters) {
            if (queryParameters != null) {
                this.errorPageQueryParams = Optional.of(Set.of(queryParameters));
            }
            return this;
        }

        /**
         * Configures the landing page to redirect to if there is no saved page to redirect back to.
         *
         * @param landingPage see the 'quarkus.http.auth.form.landing-page' configuration property
         * @return Builder
         * @see FormAuthConfig#landingPage()
         */
        public Builder landingPage(String landingPage) {
            this.landingPage = Optional.ofNullable(landingPage);
            return this;
        }

        /**
         * Configures query parameters Quarkus passes through when redirecting requests to the landing page.
         *
         * @param queryParameters query parameters; must not be null
         * @return Builder
         * @see FormAuthConfig#landingPageQueryParams()
         */
        public Builder landingPageQueryParameters(String... queryParameters) {
            if (queryParameters != null) {
                this.landingPageQueryParams = Optional.of(Set.of(queryParameters));
            }
            return this;
        }

        /**
         * Configures a name for the cookie that is used to redirect the user back to the location they want to access.
         *
         * @param locationCookie see the 'quarkus.http.auth.form.location-cookie' configuration property
         * @return Builder
         * @see FormAuthConfig#locationCookie()
         */
        public Builder locationCookie(String locationCookie) {
            Objects.requireNonNull(locationCookie);
            this.locationCookie = locationCookie;
            return this;
        }

        /**
         * Configures the inactivity timeout.
         *
         * @param timeout see the 'quarkus.http.auth.form.timeout' configuration property
         * @return Builder
         * @see FormAuthConfig#timeout()
         */
        public Builder timeout(Duration timeout) {
            Objects.requireNonNull(timeout);
            this.timeout = timeout;
            return this;
        }

        /**
         * Configures how old a cookie can get before it will be replaced with a new cookie with an updated timeout.
         *
         * @param newCookieInterval see the 'quarkus.http.auth.form.new-cookie-interval' configuration property
         * @return Builder
         * @see FormAuthConfig#newCookieInterval()
         */
        public Builder newCookieInterval(Duration newCookieInterval) {
            Objects.requireNonNull(newCookieInterval);
            this.newCookieInterval = newCookieInterval;
            return this;
        }

        /**
         * Configures a name for the cookie that is used to store the persistent session.
         *
         * @param cookieName see the 'quarkus.http.auth.form.cookie-name' configuration property
         * @return Builder
         * @see FormAuthConfig#cookieName()
         */
        public Builder cookieName(String cookieName) {
            Objects.requireNonNull(cookieName);
            this.cookieName = cookieName;
            return this;
        }

        /**
         * Configures the cookie path for the session and location cookies.
         *
         * @param cookiePath see the 'quarkus.http.auth.form.cookie-path' configuration property
         * @return Builder
         * @see FormAuthConfig#cookiePath()
         */
        public Builder cookiePath(String cookiePath) {
            this.cookiePath = Optional.ofNullable(cookiePath);
            return this;
        }

        /**
         * Configures the 'domain' attribute for the session and location cookies.
         *
         * @param cookieDomain see the 'quarkus.http.auth.form.cookie-domain' configuration property
         * @return Builder
         * @see FormAuthConfig#cookieDomain()
         */
        public Builder cookieDomain(String cookieDomain) {
            this.cookieDomain = Optional.ofNullable(cookieDomain);
            return this;
        }

        /**
         * Configures the HttpOnly attribute to prevent access to the cookie via JavaScript.
         *
         * @param httpOnlyCookie see the 'quarkus.http.auth.form.http-only-cookie' configuration property
         * @return Builder
         * @see FormAuthConfig#httpOnlyCookie()
         */
        public Builder httpOnlyCookie(boolean httpOnlyCookie) {
            this.httpOnlyCookie = httpOnlyCookie;
            return this;
        }

        /**
         * This method is a shortcut for {@code httpOnlyCookie(true)}.
         *
         * @return Builder
         * @see #httpOnlyCookie(boolean)
         */
        public Builder httpOnlyCookie() {
            return httpOnlyCookie(true);
        }

        /**
         * Configures the SameSite attribute for the session and location cookies.
         *
         * @param cookieSameSite see the 'quarkus.http.auth.form.cookie-same-site' configuration property
         * @return Builder
         * @see FormAuthConfig#cookieSameSite()
         */
        public Builder cookieSameSite(FormAuthConfig.CookieSameSite cookieSameSite) {
            Objects.requireNonNull(cookieSameSite);
            this.cookieSameSite = cookieSameSite;
            return this;
        }

        /**
         * Configures the Max-Age attribute for the session cookie.
         *
         * @param cookieMaxAge see the 'quarkus.http.auth.form.cookie-max-age' configuration property
         * @return Builder
         * @see FormAuthConfig#cookieMaxAge()
         */
        public Builder cookieMaxAge(Duration cookieMaxAge) {
            this.cookieMaxAge = Optional.ofNullable(cookieMaxAge);
            return this;
        }

        /**
         * Configures the encryption key that is used to store persistent logins for the Form-based authentication.
         *
         * @param encryptionKey see the 'quarkus.http.auth.session.encryption-key' configuration property
         * @return Builder
         * @see VertxHttpConfig#encryptionKey()
         */
        public Builder encryptionKey(String encryptionKey) {
            this.encryptionKey = Optional.ofNullable(encryptionKey);
            return this;
        }

        /**
         * Form-based authentication mechanism priority.
         *
         * @param priority {@link HttpAuthenticationMechanism#getPriority()}
         * @return Builder
         * @see FormAuthConfig#priority()
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public HttpAuthenticationMechanism build() {
            return new FormAuthenticationMechanism(createFormConfig(), encryptionKey);
        }

        private FormAuthConfig createFormConfig() {
            record FormConfigImpl(Optional<String> loginPage, String usernameParameter, String passwordParameter,
                    Optional<String> errorPage, Optional<String> landingPage,
                    String locationCookie, Duration timeout, Duration newCookieInterval, String cookieName,
                    Optional<String> cookiePath, Optional<String> cookieDomain, boolean httpOnlyCookie,
                    CookieSameSite cookieSameSite, Optional<Duration> cookieMaxAge, String postLocation,
                    Optional<Set<String>> landingPageQueryParams, Optional<Set<String>> errorPageQueryParams,
                    Optional<Set<String>> loginPageQueryParams, int priority) implements FormAuthConfig {
            }
            return new FormConfigImpl(loginPage, usernameParameter, passwordParameter, errorPage,
                    landingPage, locationCookie, timeout, newCookieInterval, cookieName, cookiePath,
                    cookieDomain, httpOnlyCookie, cookieSameSite, cookieMaxAge, postLocation, landingPageQueryParams,
                    errorPageQueryParams, loginPageQueryParams, priority);
        }
    }
}
