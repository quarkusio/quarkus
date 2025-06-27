package io.quarkus.vertx.http.runtime.security;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.vertx.http.runtime.FormAuthConfig;
import io.quarkus.vertx.http.runtime.FormAuthConfig.CookieSameSite;
import io.quarkus.vertx.http.security.event.Form;

final class FormImpl implements Form {

    private boolean enabled;
    private boolean built;
    private String postLocation;
    private Optional<String> loginPage;
    private String usernameParameter;
    private String passwordParameter;
    private Optional<String> errorPage;
    private Optional<String> landingPage;
    private boolean redirectAfterLogin;
    private String locationCookie;
    private Duration timeout;
    private Duration newCookieInterval;
    private String cookieName;
    private Optional<String> cookiePath;
    private Optional<String> cookieDomain;
    private boolean httpOnlyCookie;
    private CookieSameSite cookieSameSite;
    private Optional<Duration> cookieMaxAge;
    private Optional<String> encryptionKey;

    FormImpl(boolean enabled, FormAuthConfig formAuthConfig, Optional<String> encryptionKey) {
        this.enabled = enabled;
        this.built = false;
        this.postLocation = formAuthConfig.postLocation();
        this.loginPage = formAuthConfig.loginPage();
        this.usernameParameter = formAuthConfig.usernameParameter();
        this.passwordParameter = formAuthConfig.passwordParameter();
        this.errorPage = formAuthConfig.errorPage();
        this.landingPage = formAuthConfig.landingPage();
        this.redirectAfterLogin = formAuthConfig.redirectAfterLogin();
        this.locationCookie = formAuthConfig.locationCookie();
        this.timeout = formAuthConfig.timeout();
        this.newCookieInterval = formAuthConfig.newCookieInterval();
        this.cookieName = formAuthConfig.cookieName();
        this.cookiePath = formAuthConfig.cookiePath();
        this.cookieDomain = formAuthConfig.cookieDomain();
        this.httpOnlyCookie = formAuthConfig.httpOnlyCookie();
        this.cookieSameSite = formAuthConfig.cookieSameSite();
        this.cookieMaxAge = formAuthConfig.cookieMaxAge();
        this.encryptionKey = encryptionKey;
    }

    @Override
    public Form enable() {
        assertNotBuilt();
        this.enabled = true;
        return this;
    }

    @Override
    public Form postLocation(String postLocation) {
        assertNotBuilt();
        this.postLocation = Objects.requireNonNull(postLocation);
        return this;
    }

    @Override
    public Form loginPage(String loginPage) {
        assertNotBuilt();
        this.loginPage = Optional.ofNullable(loginPage);
        return this;
    }

    @Override
    public Form usernameParameter(String usernameParameter) {
        assertNotBuilt();
        this.usernameParameter = Objects.requireNonNull(usernameParameter);
        return this;
    }

    @Override
    public Form passwordParameter(String passwordParameter) {
        assertNotBuilt();
        this.passwordParameter = Objects.requireNonNull(passwordParameter);
        return this;
    }

    @Override
    public Form errorPage(String errorPage) {
        assertNotBuilt();
        this.errorPage = Optional.ofNullable(errorPage);
        return this;
    }

    @Override
    public Form landingPage(String landingPage) {
        assertNotBuilt();
        this.landingPage = Optional.ofNullable(landingPage);
        return this;
    }

    @Override
    public Form locationCookie(String locationCookie) {
        assertNotBuilt();
        this.locationCookie = Objects.requireNonNull(locationCookie);
        return this;
    }

    @Override
    public Form timeout(Duration timeout) {
        assertNotBuilt();
        this.timeout = Objects.requireNonNull(timeout);
        return this;
    }

    @Override
    public Form newCookieInterval(Duration newCookieInterval) {
        assertNotBuilt();
        this.newCookieInterval = Objects.requireNonNull(newCookieInterval);
        return this;
    }

    @Override
    public Form cookieName(String cookieName) {
        assertNotBuilt();
        this.cookieName = Objects.requireNonNull(cookieName);
        return this;
    }

    @Override
    public Form cookiePath(String cookiePath) {
        assertNotBuilt();
        this.cookiePath = Optional.ofNullable(cookiePath);
        return this;
    }

    @Override
    public Form cookieDomain(String cookieDomain) {
        assertNotBuilt();
        this.cookieDomain = Optional.ofNullable(cookieDomain);
        return this;
    }

    @Override
    public Form httpOnlyCookie(boolean httpOnlyCookie) {
        assertNotBuilt();
        this.httpOnlyCookie = httpOnlyCookie;
        return this;
    }

    @Override
    public Form httpOnlyCookie() {
        return httpOnlyCookie(true);
    }

    @Override
    public Form cookieSameSite(CookieSameSite cookieSameSite) {
        assertNotBuilt();
        this.cookieSameSite = Objects.requireNonNull(cookieSameSite);
        return this;
    }

    @Override
    public Form cookieMaxAge(Duration cookieMaxAge) {
        assertNotBuilt();
        this.cookieMaxAge = Optional.ofNullable(cookieMaxAge);
        return this;
    }

    @Override
    public Form encryptionKey(String encryptionKey) {
        assertNotBuilt();
        this.encryptionKey = Optional.ofNullable(encryptionKey);
        return this;
    }

    FormImpl build() {
        assertNotBuilt();
        this.built = true;
        return this;
    }

    boolean isEnabled() {
        return enabled;
    }

    String getPostLocation() {
        return postLocation;
    }

    Optional<String> getLoginPage() {
        return loginPage;
    }

    String getUsernameParameter() {
        return usernameParameter;
    }

    String getPasswordParameter() {
        return passwordParameter;
    }

    Optional<String> getErrorPage() {
        return errorPage;
    }

    Optional<String> getLandingPage() {
        return landingPage;
    }

    boolean isRedirectAfterLogin() {
        return redirectAfterLogin;
    }

    String getLocationCookie() {
        return locationCookie;
    }

    Duration getTimeout() {
        return timeout;
    }

    Duration getNewCookieInterval() {
        return newCookieInterval;
    }

    String getCookieName() {
        return cookieName;
    }

    Optional<String> getCookiePath() {
        return cookiePath;
    }

    Optional<String> getCookieDomain() {
        return cookieDomain;
    }

    boolean isHttpOnlyCookie() {
        return httpOnlyCookie;
    }

    CookieSameSite getCookieSameSite() {
        return cookieSameSite;
    }

    Optional<Duration> getCookieMaxAge() {
        return cookieMaxAge;
    }

    Optional<String> getEncryptionKey() {
        return encryptionKey;
    }

    private void assertNotBuilt() {
        if (this.built) {
            throw new IllegalStateException("Form-based authentication has already been built");
        }
    }
}
