package io.quarkus.oidc.runtime.builders;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.oidc.OidcTenantConfigBuilder;
import io.quarkus.oidc.runtime.OidcTenantConfig.Authentication;
import io.quarkus.oidc.runtime.OidcTenantConfig.Authentication.CookieSameSite;
import io.quarkus.oidc.runtime.OidcTenantConfig.Authentication.ResponseMode;

/**
 * Builder for the {@link Authentication} config.
 */
public final class AuthenticationConfigBuilder {

    private record AuthenticationImpl(Optional<ResponseMode> responseMode, Optional<String> redirectPath,
            boolean restorePathAfterRedirect, boolean removeRedirectParameters, Optional<String> errorPath,
            Optional<String> sessionExpiredPath, boolean verifyAccessToken, Optional<Boolean> forceRedirectHttpsScheme,
            Optional<List<String>> scopes, Optional<String> scopeSeparator, boolean nonceRequired,
            Optional<Boolean> addOpenidScope, Map<String, String> extraParams, Optional<List<String>> forwardParams,
            boolean cookieForceSecure, Optional<String> cookieSuffix, String cookiePath, Optional<String> cookiePathHeader,
            Optional<String> cookieDomain, CookieSameSite cookieSameSite, boolean allowMultipleCodeFlows,
            boolean failOnMissingStateParam, boolean failOnUnresolvedKid, Optional<Boolean> userInfoRequired,
            Duration sessionAgeExtension,
            Duration stateCookieAge, boolean javaScriptAutoRedirect, Optional<Boolean> idTokenRequired,
            Optional<Duration> internalIdTokenLifespan, Optional<Boolean> pkceRequired, Optional<String> pkceSecret,
            Optional<String> stateSecret) implements Authentication {
    }

    private final OidcTenantConfigBuilder builder;
    private final Map<String, String> extraParams = new HashMap<>();
    private final List<String> forwardParams = new ArrayList<>();
    private final List<String> scopes = new ArrayList<>();
    private Optional<ResponseMode> responseMode;
    private Optional<String> redirectPath;
    private boolean restorePathAfterRedirect;
    private boolean removeRedirectParameters;
    private Optional<String> errorPath;
    private Optional<String> sessionExpiredPath;
    private boolean verifyAccessToken;
    private Optional<Boolean> forceRedirectHttpsScheme;
    private Optional<String> scopeSeparator;
    private boolean nonceRequired;
    private Optional<Boolean> addOpenidScope;
    private boolean cookieForceSecure;
    private Optional<String> cookieSuffix;
    private String cookiePath;
    private Optional<String> cookiePathHeader;
    private Optional<String> cookieDomain;
    private CookieSameSite cookieSameSite;
    private boolean allowMultipleCodeFlows;
    private boolean failOnMissingStateParam;
    private boolean failOnUnresolvedKid;
    private Optional<Boolean> userInfoRequired;
    private Duration sessionAgeExtension;
    private Duration stateCookieAge;
    private boolean javaScriptAutoRedirect;
    private Optional<Boolean> idTokenRequired;
    private Optional<Duration> internalIdTokenLifespan;
    private Optional<Boolean> pkceRequired;
    private Optional<String> pkceSecret;
    private Optional<String> stateSecret;

    public AuthenticationConfigBuilder() {
        this(new OidcTenantConfigBuilder());
    }

    public AuthenticationConfigBuilder(OidcTenantConfigBuilder builder) {
        this.builder = Objects.requireNonNull(builder);
        var authentication = builder.getAuthentication();
        extraParams.putAll(authentication.extraParams());
        if (authentication.forwardParams().isPresent()) {
            forwardParams.addAll(authentication.forwardParams().get());
        }
        if (authentication.scopes().isPresent()) {
            scopes.addAll(authentication.scopes().get());
        }
        this.responseMode = authentication.responseMode();
        this.redirectPath = authentication.redirectPath();
        this.restorePathAfterRedirect = authentication.restorePathAfterRedirect();
        this.removeRedirectParameters = authentication.removeRedirectParameters();
        this.errorPath = authentication.errorPath();
        this.sessionExpiredPath = authentication.sessionExpiredPath();
        this.verifyAccessToken = authentication.verifyAccessToken();
        this.forceRedirectHttpsScheme = authentication.forceRedirectHttpsScheme();
        this.scopeSeparator = authentication.scopeSeparator();
        this.nonceRequired = authentication.nonceRequired();
        this.addOpenidScope = authentication.addOpenidScope();
        this.cookieForceSecure = authentication.cookieForceSecure();
        this.cookieSuffix = authentication.cookieSuffix();
        this.cookiePath = authentication.cookiePath();
        this.cookiePathHeader = authentication.cookiePathHeader();
        this.cookieDomain = authentication.cookieDomain();
        this.cookieSameSite = authentication.cookieSameSite();
        this.allowMultipleCodeFlows = authentication.allowMultipleCodeFlows();
        this.failOnMissingStateParam = authentication.failOnMissingStateParam();
        this.failOnUnresolvedKid = authentication.failOnUnresolvedKid();
        this.userInfoRequired = authentication.userInfoRequired();
        this.sessionAgeExtension = authentication.sessionAgeExtension();
        this.stateCookieAge = authentication.stateCookieAge();
        this.javaScriptAutoRedirect = authentication.javaScriptAutoRedirect();
        this.idTokenRequired = authentication.idTokenRequired();
        this.internalIdTokenLifespan = authentication.internalIdTokenLifespan();
        this.pkceRequired = authentication.pkceRequired();
        this.pkceSecret = authentication.pkceSecret();
        this.stateSecret = authentication.stateSecret();
    }

    /**
     * @param responseMode {@link Authentication#responseMode()}
     * @return this builder
     */
    public AuthenticationConfigBuilder responseMode(ResponseMode responseMode) {
        this.responseMode = Optional.ofNullable(responseMode);
        return this;
    }

    /**
     * @param redirectPath {@link Authentication#redirectPath()}
     * @return this builder
     */
    public AuthenticationConfigBuilder redirectPath(String redirectPath) {
        this.redirectPath = Optional.ofNullable(redirectPath);
        return this;
    }

    /**
     * Sets {@link Authentication#restorePathAfterRedirect()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder restorePathAfterRedirect() {
        return restorePathAfterRedirect(true);
    }

    /**
     * @param restorePathAfterRedirect {@link Authentication#restorePathAfterRedirect()}
     * @return this builder
     */
    public AuthenticationConfigBuilder restorePathAfterRedirect(boolean restorePathAfterRedirect) {
        this.restorePathAfterRedirect = restorePathAfterRedirect;
        return this;
    }

    /**
     * Sets {@link Authentication#removeRedirectParameters()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder removeRedirectParameters() {
        return removeRedirectParameters(true);
    }

    /**
     * @param removeRedirectParameters {@link Authentication#removeRedirectParameters()}
     * @return this builder
     */
    public AuthenticationConfigBuilder removeRedirectParameters(boolean removeRedirectParameters) {
        this.removeRedirectParameters = removeRedirectParameters;
        return this;
    }

    /**
     * @param errorPath {@link Authentication#errorPath()}
     * @return this builder
     */
    public AuthenticationConfigBuilder errorPath(String errorPath) {
        this.errorPath = Optional.ofNullable(errorPath);
        return this;
    }

    /**
     * @param sessionExpiredPath {@link Authentication#sessionExpiredPath()}
     * @return this builder
     */
    public AuthenticationConfigBuilder sessionExpiredPath(String sessionExpiredPath) {
        this.sessionExpiredPath = Optional.ofNullable(sessionExpiredPath);
        return this;
    }

    /**
     * Sets {@link Authentication#verifyAccessToken()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder verifyAccessToken() {
        return verifyAccessToken(true);
    }

    /**
     * @param verifyAccessToken {@link Authentication#verifyAccessToken()}
     * @return this builder
     */
    public AuthenticationConfigBuilder verifyAccessToken(boolean verifyAccessToken) {
        this.verifyAccessToken = verifyAccessToken;
        return this;
    }

    /**
     * Sets {@link Authentication#forceRedirectHttpsScheme()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder forceRedirectHttpsScheme() {
        return forceRedirectHttpsScheme(true);
    }

    /**
     * @param forceRedirectHttpsScheme {@link Authentication#forceRedirectHttpsScheme()}
     * @return this builder
     */
    public AuthenticationConfigBuilder forceRedirectHttpsScheme(boolean forceRedirectHttpsScheme) {
        this.forceRedirectHttpsScheme = Optional.of(forceRedirectHttpsScheme);
        return this;
    }

    /**
     * @param scopes {@link Authentication#scopes()}
     * @return this builder
     */
    public AuthenticationConfigBuilder scopes(List<String> scopes) {
        if (scopes != null) {
            this.scopes.addAll(scopes);
        }
        return this;
    }

    /**
     * @param scopes {@link Authentication#scopes()}
     * @return this builder
     */
    public AuthenticationConfigBuilder scopes(String... scopes) {
        if (scopes != null) {
            this.scopes.addAll(Arrays.asList(scopes));
        }
        return this;
    }

    /**
     * @param separator {@link Authentication#scopeSeparator()}
     * @return this builder
     */
    public AuthenticationConfigBuilder scopeSeparator(String separator) {
        this.scopeSeparator = Optional.ofNullable(separator);
        return this;
    }

    /**
     * Sets {@link Authentication#nonceRequired()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder nonceRequired() {
        return nonceRequired(true);
    }

    /**
     * @param nonceRequired {@link Authentication#nonceRequired()}
     * @return this builder
     */
    public AuthenticationConfigBuilder nonceRequired(boolean nonceRequired) {
        this.nonceRequired = nonceRequired;
        return this;
    }

    /**
     * Sets {@link Authentication#addOpenidScope()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder addOpenidScope() {
        return addOpenidScope(true);
    }

    /**
     * @param addOpenidScope {@link Authentication#addOpenidScope()}
     * @return this builder
     */
    public AuthenticationConfigBuilder addOpenidScope(boolean addOpenidScope) {
        this.addOpenidScope = Optional.of(addOpenidScope);
        return this;
    }

    /**
     * @param forwardParams {@link Authentication#forwardParams()}
     * @return this builder
     */
    public AuthenticationConfigBuilder forwardParams(List<String> forwardParams) {
        if (forwardParams != null) {
            this.forwardParams.addAll(forwardParams);
        }
        return this;
    }

    /**
     * @param forwardParams {@link Authentication#forwardParams()}
     * @return this builder
     */
    public AuthenticationConfigBuilder forwardParams(String... forwardParams) {
        if (forwardParams != null) {
            this.forwardParams.addAll(Arrays.asList(forwardParams));
        }
        return this;
    }

    /**
     * Sets {@link Authentication#cookieForceSecure()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder cookieForceSecure() {
        return cookieForceSecure(true);
    }

    /**
     * @param cookieForceSecure {@link Authentication#cookieForceSecure()}
     * @return this builder
     */
    public AuthenticationConfigBuilder cookieForceSecure(boolean cookieForceSecure) {
        this.cookieForceSecure = cookieForceSecure;
        return this;
    }

    /**
     * @param cookieSuffix {@link Authentication#cookieSuffix()}
     * @return this builder
     */
    public AuthenticationConfigBuilder cookieSuffix(String cookieSuffix) {
        this.cookieSuffix = Optional.ofNullable(cookieSuffix);
        return this;
    }

    /**
     * @param cookiePath {@link Authentication#cookiePath()}
     * @return this builder
     */
    public AuthenticationConfigBuilder cookiePath(String cookiePath) {
        this.cookiePath = Objects.requireNonNull(cookiePath);
        return this;
    }

    /**
     * @param cookiePathHeader {@link Authentication#cookiePathHeader()}
     * @return this builder
     */
    public AuthenticationConfigBuilder cookiePathHeader(String cookiePathHeader) {
        this.cookiePathHeader = Optional.ofNullable(cookiePathHeader);
        return this;
    }

    /**
     * @param cookieDomain {@link Authentication#cookieDomain()}
     * @return this builder
     */
    public AuthenticationConfigBuilder cookieDomain(String cookieDomain) {
        this.cookieDomain = Optional.ofNullable(cookieDomain);
        return this;
    }

    /**
     * @param cookieSameSite {@link Authentication#cookieSameSite()}
     * @return this builder
     */
    public AuthenticationConfigBuilder cookieSameSite(CookieSameSite cookieSameSite) {
        this.cookieSameSite = Objects.requireNonNull(cookieSameSite);
        return this;
    }

    /**
     * Sets {@link Authentication#allowMultipleCodeFlows()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder allowMultipleCodeFlows() {
        return allowMultipleCodeFlows(true);
    }

    /**
     * @param allowMultipleCodeFlows {@link Authentication#allowMultipleCodeFlows()}
     * @return this builder
     */
    public AuthenticationConfigBuilder allowMultipleCodeFlows(boolean allowMultipleCodeFlows) {
        this.allowMultipleCodeFlows = allowMultipleCodeFlows;
        return this;
    }

    /**
     * Sets {@link Authentication#failOnMissingStateParam()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder failOnMissingStateParam() {
        return failOnMissingStateParam(true);
    }

    /**
     * @param failOnMissingStateParam {@link Authentication#failOnMissingStateParam()}
     * @return this builder
     */
    public AuthenticationConfigBuilder failOnMissingStateParam(boolean failOnMissingStateParam) {
        this.failOnMissingStateParam = failOnMissingStateParam;
        return this;
    }

    /**
     * Sets {@link Authentication#failOnUnreslvedKid()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder failOnUnresolvedKid() {
        return failOnUnresolvedKid(true);
    }

    /**
     * @param failOnUnresolvedKid {@link Authentication#failOnUnreslvedKid()}
     * @return this builder
     */
    public AuthenticationConfigBuilder failOnUnresolvedKid(boolean failOnUnresolvedKid) {
        this.failOnUnresolvedKid = failOnUnresolvedKid;
        return this;
    }

    /**
     * Sets {@link Authentication#userInfoRequired()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder userInfoRequired() {
        return userInfoRequired(true);
    }

    /**
     * @param userInfoRequired {@link Authentication#userInfoRequired()}
     * @return this builder
     */
    public AuthenticationConfigBuilder userInfoRequired(boolean userInfoRequired) {
        this.userInfoRequired = Optional.of(userInfoRequired);
        return this;
    }

    /**
     * @param sessionAgeExtension {@link Authentication#sessionAgeExtension()}
     * @return this builder
     */
    public AuthenticationConfigBuilder sessionAgeExtension(Duration sessionAgeExtension) {
        this.sessionAgeExtension = Objects.requireNonNull(sessionAgeExtension);
        return this;
    }

    /**
     * @param stateCookieAge {@link Authentication#stateCookieAge()}
     * @return this builder
     */
    public AuthenticationConfigBuilder stateCookieAge(Duration stateCookieAge) {
        this.stateCookieAge = Objects.requireNonNull(stateCookieAge);
        return this;
    }

    /**
     * Sets {@link Authentication#javaScriptAutoRedirect()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder javaScriptAutoRedirect() {
        return javaScriptAutoRedirect(true);
    }

    /**
     * @param javaScriptAutoRedirect {@link Authentication#javaScriptAutoRedirect()}
     * @return this builder
     */
    public AuthenticationConfigBuilder javaScriptAutoRedirect(boolean javaScriptAutoRedirect) {
        this.javaScriptAutoRedirect = javaScriptAutoRedirect;
        return this;
    }

    /**
     * Sets {@link Authentication#idTokenRequired()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder idTokenRequired() {
        return idTokenRequired(true);
    }

    /**
     * @param idTokenRequired {@link Authentication#idTokenRequired()}
     * @return this builder
     */
    public AuthenticationConfigBuilder idTokenRequired(boolean idTokenRequired) {
        this.idTokenRequired = Optional.of(idTokenRequired);
        return this;
    }

    /**
     * @param internalIdTokenLifespan {@link Authentication#internalIdTokenLifespan()}
     * @return this builder
     */
    public AuthenticationConfigBuilder internalIdTokenLifespan(Duration internalIdTokenLifespan) {
        this.internalIdTokenLifespan = Optional.ofNullable(internalIdTokenLifespan);
        return this;
    }

    /**
     * @param extraParams {@link Authentication#extraParams()}
     * @return this builder
     */
    public AuthenticationConfigBuilder extraParams(Map<String, String> extraParams) {
        if (extraParams != null) {
            this.extraParams.putAll(extraParams);
        }
        return this;
    }

    /**
     * @param key {@link Authentication#extraParams()} key
     * @param value {@link Authentication#extraParams()} value
     * @return this builder
     */
    public AuthenticationConfigBuilder extraParam(String key, String value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        extraParams.put(key, value);
        return this;
    }

    /**
     * Sets {@link Authentication#pkceRequired()} to true.
     *
     * @return this builder
     */
    public AuthenticationConfigBuilder pkceRequired() {
        return pkceRequired(true);
    }

    /**
     * @param pkceRequired {@link Authentication#pkceRequired()}
     * @return this builder
     */
    public AuthenticationConfigBuilder pkceRequired(boolean pkceRequired) {
        this.pkceRequired = Optional.of(pkceRequired);
        return this;
    }

    /**
     * @param stateSecret {@link Authentication#stateSecret()}
     * @return this builder
     */
    public AuthenticationConfigBuilder stateSecret(String stateSecret) {
        this.stateSecret = Optional.ofNullable(stateSecret);
        return this;
    }

    /**
     * @return OidcTenantConfigBuilder with built {@link Authentication}
     */
    public OidcTenantConfigBuilder end() {
        return builder.authentication(build());
    }

    /**
     * @return builds {@link Authentication}
     */
    public Authentication build() {
        Optional<List<String>> optionalScopes = scopes.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(scopes));
        Optional<List<String>> optionalForwardParams = forwardParams.isEmpty() ? Optional.empty()
                : Optional.of(List.copyOf(forwardParams));
        return new AuthenticationImpl(responseMode, redirectPath, restorePathAfterRedirect, removeRedirectParameters, errorPath,
                sessionExpiredPath, verifyAccessToken, forceRedirectHttpsScheme, optionalScopes, scopeSeparator, nonceRequired,
                addOpenidScope, Map.copyOf(extraParams), optionalForwardParams, cookieForceSecure, cookieSuffix, cookiePath,
                cookiePathHeader, cookieDomain, cookieSameSite, allowMultipleCodeFlows, failOnMissingStateParam,
                failOnUnresolvedKid,
                userInfoRequired, sessionAgeExtension, stateCookieAge, javaScriptAutoRedirect, idTokenRequired,
                internalIdTokenLifespan, pkceRequired, pkceSecret, stateSecret);
    }
}
