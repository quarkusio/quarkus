package io.quarkus.oidc;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import io.quarkus.oidc.common.runtime.OidcCommonConfig;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.quarkus.security.identity.SecurityIdentityAugmentor;

@ConfigGroup
public class OidcTenantConfig extends OidcCommonConfig {

    /**
     * A unique tenant identifier. It must be set by {@code TenantConfigResolver} providers which
     * resolve the tenant configuration dynamically and is optional in all other cases.
     */
    @ConfigItem
    public Optional<String> tenantId = Optional.empty();

    /**
     * If this tenant configuration is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean tenantEnabled = true;

    /**
     * The application type, which can be one of the following values from enum {@link ApplicationType}.
     */
    @ConfigItem(defaultValueDocumentation = "service")
    public Optional<ApplicationType> applicationType = Optional.empty();

    /**
     * Relative path or absolute URL of the OIDC authorization endpoint which authenticates the users.
     * This property must be set for the 'web-app' applications if OIDC discovery is disabled.
     * This property will be ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> authorizationPath = Optional.empty();

    /**
     * Relative path or absolute URL of the OIDC userinfo endpoint.
     * This property must only be set for the 'web-app' applications if OIDC discovery is disabled
     * and 'authentication.user-info-required' property is enabled.
     * This property will be ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> userInfoPath = Optional.empty();

    /**
     * Relative path or absolute URL of the OIDC RFC7662 introspection endpoint which can introspect both opaque and JWT tokens.
     * This property must be set if OIDC discovery is disabled and 1) the opaque bearer access tokens have to be verified
     * or 2) JWT tokens have to be verified while the cached JWK verification set with no matching JWK is being refreshed.
     * This property will be ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> introspectionPath = Optional.empty();

    /**
     * Relative path or absolute URL of the OIDC JWKS endpoint which returns a JSON Web Key Verification Set.
     * This property should be set if OIDC discovery is disabled and the local JWT verification is required.
     * This property will be ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> jwksPath = Optional.empty();

    /**
     * Relative path or absolute URL of the OIDC end_session_endpoint.
     * This property must be set if OIDC discovery is disabled and RP Initiated Logout support for the 'web-app' applications is
     * required.
     * This property will be ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> endSessionPath = Optional.empty();

    /**
     * Public key for the local JWT token verification.
     * OIDC server connection will not be created when this property is set.
     */
    @ConfigItem
    public Optional<String> publicKey = Optional.empty();

    /**
     * Introspection Basic Authentication which must be configured only if the introspection is required
     * and OpenId Connect Provider does not support the OIDC client authentication configured with
     * {@link OidcCommonConfig#credentials} for its introspection endpoint.
     */
    @ConfigItem
    public IntrospectionCredentials introspectionCredentials = new IntrospectionCredentials();

    /**
     * Introspection Basic Authentication configuration
     */
    @ConfigGroup
    public static class IntrospectionCredentials {
        /**
         * Name
         */
        @ConfigItem
        public Optional<String> name = Optional.empty();

        /**
         * Secret
         */
        @ConfigItem
        public Optional<String> secret = Optional.empty();

        /**
         * Include OpenId Connect Client ID configured with 'quarkus.oidc.client-id'
         */
        @ConfigItem(defaultValue = "true")
        public boolean includeClientId = true;

        public Optional<String> getName() {
            return name;
        }

        public void setName(String name) {
            this.name = Optional.of(name);
        }

        public Optional<String> getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = Optional.of(secret);
        }

        public boolean isIncludeClientId() {
            return includeClientId;
        }

        public void setIncludeClientId(boolean includeClientId) {
            this.includeClientId = includeClientId;
        }

    }

    /**
     * Configuration to find and parse a custom claim containing the roles information.
     */
    @ConfigItem
    public Roles roles = new Roles();

    /**
     * Configuration how to validate the token claims.
     */
    @ConfigItem
    public Token token = new Token();

    /**
     * RP Initiated, BackChannel and FrontChannel Logout configuration
     */
    @ConfigItem
    public Logout logout = new Logout();

    /**
     * Different options to configure authorization requests
     */
    public Authentication authentication = new Authentication();

    /**
     * Authorization code grant configuration
     */
    public CodeGrant codeGrant = new CodeGrant();

    /**
     * Default token state manager configuration
     */
    @ConfigItem
    public TokenStateManager tokenStateManager = new TokenStateManager();

    /**
     * Allow caching the token introspection data.
     * Note enabling this property does not enable the cache itself but only permits to cache the token introspection
     * for a given tenant. If the default token cache can be used then please see {@link OidcConfig.TokenCache} how to enable
     * it.
     */
    @ConfigItem(defaultValue = "true")
    public boolean allowTokenIntrospectionCache = true;

    /**
     * Allow caching the user info data.
     * Note enabling this property does not enable the cache itself but only permits to cache the user info data
     * for a given tenant. If the default token cache can be used then please see {@link OidcConfig.TokenCache} how to enable
     * it.
     */
    @ConfigItem(defaultValue = "true")
    public boolean allowUserInfoCache = true;

    /**
     * Allow inlining UserInfo in IdToken instead of caching it in the token cache.
     * This property is only checked when an internal IdToken is generated when Oauth2 providers do not return IdToken.
     * Inlining UserInfo in the generated IdToken allows to store it in the session cookie and avoids introducing a cached
     * state.
     */
    @ConfigItem(defaultValue = "false")
    public boolean cacheUserInfoInIdtoken = false;

    @ConfigGroup
    public static class Logout {

        /**
         * The relative path of the logout endpoint at the application. If provided, the application is able to initiate the
         * logout through this endpoint in conformance with the OpenID Connect RP-Initiated Logout specification.
         */
        @ConfigItem
        public Optional<String> path = Optional.empty();

        /**
         * Relative path of the application endpoint where the user should be redirected to after logging out from the OpenID
         * Connect Provider.
         * This endpoint URI must be properly registered at the OpenID Connect Provider as a valid redirect URI.
         */
        @ConfigItem
        public Optional<String> postLogoutPath = Optional.empty();

        /**
         * Name of the post logout URI parameter which will be added as a query parameter to the logout redirect URI.
         */
        @ConfigItem(defaultValue = OidcConstants.POST_LOGOUT_REDIRECT_URI)
        public String postLogoutUriParam;

        /**
         * Additional properties which will be added as the query parameters to the logout redirect URI.
         */
        @ConfigItem
        public Map<String, String> extraParams;

        /**
         * Back-Channel Logout configuration
         */
        @ConfigItem
        public Backchannel backchannel = new Backchannel();

        /**
         * Front-Channel Logout configuration
         */
        @ConfigItem
        public Frontchannel frontchannel = new Frontchannel();

        public void setPath(Optional<String> path) {
            this.path = path;
        }

        public String getPath() {
            return path.get();
        }

        public void setPostLogoutPath(Optional<String> postLogoutPath) {
            this.postLogoutPath = postLogoutPath;
        }

        public Optional<String> getPostLogoutPath() {
            return postLogoutPath;
        }

        public Map<String, String> getExtraParams() {
            return extraParams;
        }

        public void setExtraParams(Map<String, String> extraParams) {
            this.extraParams = extraParams;
        }

        public String getPostLogoutUriParam() {
            return postLogoutUriParam;
        }

        public void setPostLogoutUriParam(String postLogoutUriParam) {
            this.postLogoutUriParam = postLogoutUriParam;
        }

        public Backchannel getBackchannel() {
            return backchannel;
        }

        public void setBackchannel(Backchannel backchannel) {
            this.backchannel = backchannel;
        }

        public Frontchannel getFrontchannel() {
            return frontchannel;
        }

        public void setFrontchannel(Frontchannel frontchannel) {
            this.frontchannel = frontchannel;
        }
    }

    @ConfigGroup
    public static class Backchannel {
        /**
         * The relative path of the Back-Channel Logout endpoint at the application.
         */
        @ConfigItem
        public Optional<String> path = Optional.empty();

        public void setPath(Optional<String> path) {
            this.path = path;
        }

        public String getPath() {
            return path.get();
        }
    }

    @ConfigGroup
    public static class Frontchannel {
        /**
         * The relative path of the Front-Channel Logout endpoint at the application.
         */
        @ConfigItem
        public Optional<String> path = Optional.empty();

        public void setPath(Optional<String> path) {
            this.path = path;
        }

        public String getPath() {
            return path.get();
        }
    }

    /**
     * Default Authorization Code token state manager configuration
     */
    @ConfigGroup
    public static class TokenStateManager {

        public enum Strategy {
            /**
             * Keep ID, access and refresh tokens.
             */
            KEEP_ALL_TOKENS,

            /**
             * Keep ID token only
             */
            ID_TOKEN,

            /**
             * Keep ID and refresh tokens only
             */
            ID_REFRESH_TOKENS
        }

        /**
         * Default TokenStateManager strategy.
         */
        @ConfigItem(defaultValue = "keep_all_tokens")
        public Strategy strategy = Strategy.KEEP_ALL_TOKENS;

        /**
         * Default TokenStateManager keeps all tokens (ID, access and refresh)
         * returned in the authorization code grant response in a single session cookie by default.
         *
         * Enable this property to minimize a session cookie size
         */
        @ConfigItem(defaultValue = "false")
        public boolean splitTokens;

        /**
         * Requires that the tokens are encrypted before being stored in the cookies.
         */
        @ConfigItem(defaultValueDocumentation = "false")
        public Optional<Boolean> encryptionRequired = Optional.empty();

        /**
         * Secret which will be used to encrypt the tokens.
         * This secret must be set if the token encryption is required but no client secret is set.
         * The length of the secret which will be used to encrypt the tokens must be 32 characters long.
         */
        @ConfigItem
        public Optional<String> encryptionSecret = Optional.empty();

        public Optional<Boolean> isEncryptionRequired() {
            return encryptionRequired;
        }

        public void setEncryptionRequired(boolean encryptionRequired) {
            this.encryptionRequired = Optional.of(encryptionRequired);
        }

        public Optional<String> getEncryptionSecret() {
            return encryptionSecret;
        }

        public void setEncryptionSecret(String encryptionSecret) {
            this.encryptionSecret = Optional.of(encryptionSecret);
        }

        public boolean isSplitTokens() {
            return splitTokens;
        }

        public void setSplitTokens(boolean splitTokens) {
            this.splitTokens = splitTokens;
        }

        public Strategy getStrategy() {
            return strategy;
        }

        public void setStrategy(Strategy strategy) {
            this.strategy = strategy;
        }
    }

    public Optional<String> getAuthorizationPath() {
        return authorizationPath;
    }

    public void setAuthorizationPath(String authorizationPath) {
        this.authorizationPath = Optional.of(authorizationPath);
    }

    public Optional<String> getUserInfoPath() {
        return userInfoPath;
    }

    public void setUserInfoPath(String userInfoPath) {
        this.userInfoPath = Optional.of(userInfoPath);
    }

    public Optional<String> getIntrospectionPath() {
        return introspectionPath;
    }

    public void setIntrospectionPath(String introspectionPath) {
        this.introspectionPath = Optional.of(introspectionPath);
    }

    public Optional<String> getJwksPath() {
        return jwksPath;
    }

    public void setJwksPath(String jwksPath) {
        this.jwksPath = Optional.of(jwksPath);
    }

    public Optional<String> getEndSessionPath() {
        return endSessionPath;
    }

    public void setEndSessionPath(String endSessionPath) {
        this.endSessionPath = Optional.of(endSessionPath);
    }

    public Optional<String> getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = Optional.of(publicKey);
    }

    public Roles getRoles() {
        return roles;
    }

    public void setRoles(Roles roles) {
        this.roles = roles;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public Optional<String> getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = Optional.of(tenantId);
    }

    public boolean isTenantEnabled() {
        return tenantEnabled;
    }

    public void setTenantEnabled(boolean enabled) {
        this.tenantEnabled = enabled;
    }

    public void setLogout(Logout logout) {
        this.logout = logout;
    }

    public Logout getLogout() {
        return logout;
    }

    @ConfigGroup
    public static class Roles {

        public static Roles fromClaimPath(List<String> path) {
            return fromClaimPathAndSeparator(path, null);
        }

        public static Roles fromClaimPathAndSeparator(List<String> path, String sep) {
            Roles roles = new Roles();
            roles.roleClaimPath = Optional.ofNullable(path);
            roles.roleClaimSeparator = Optional.ofNullable(sep);
            return roles;
        }

        /**
         * List of paths to claims containing an array of groups. Each path starts from the top level JWT JSON object
         * and can contain multiple segments where each segment represents a JSON object name only,
         * example: "realm/groups". Use double quotes with the namespace qualified claim names.
         * This property can be used if a token has no 'groups' claim but has the groups set in one or more different
         * claims.
         */
        @ConfigItem
        public Optional<List<String>> roleClaimPath = Optional.empty();
        /**
         * Separator for splitting a string which may contain multiple group values.
         * It will only be used if the "role-claim-path" property points to one or more custom claims whose values are strings.
         * A single space will be used by default because the standard 'scope' claim may contain a space separated sequence.
         */
        @ConfigItem
        public Optional<String> roleClaimSeparator = Optional.empty();

        /**
         * Source of the principal roles.
         */
        @ConfigItem
        public Optional<Source> source = Optional.empty();

        public Optional<List<String>> getRoleClaimPath() {
            return roleClaimPath;
        }

        public void setRoleClaimPath(List<String> roleClaimPath) {
            this.roleClaimPath = Optional.of(roleClaimPath);
        }

        public Optional<String> getRoleClaimSeparator() {
            return roleClaimSeparator;
        }

        public void setRoleClaimSeparator(String roleClaimSeparator) {
            this.roleClaimSeparator = Optional.of(roleClaimSeparator);
        }

        public Optional<Source> getSource() {
            return source;
        }

        public void setSource(Source source) {
            this.source = Optional.of(source);
        }

        // Source of the principal roles
        public static enum Source {
            /**
             * ID Token - the default value for the 'web-app' applications.
             */
            idtoken,

            /**
             * Access Token - the default value for the 'service' applications;
             * can also be used as the source of roles for the 'web-app' applications.
             */
            accesstoken,

            /**
             * User Info
             */
            userinfo
        }
    }

    /**
     * Defines the authorization request properties when authenticating
     * users using the Authorization Code Grant Type.
     */
    @ConfigGroup
    public static class Authentication {

        /**
         * Authorization code flow response mode
         */
        public enum ResponseMode {
            /**
             * Authorization response parameters are encoded in the query string added to the redirect_uri
             */
            QUERY,

            /**
             * Authorization response parameters are encoded as HTML form values that are auto-submitted in the browser
             * and transmitted via the HTTP POST method using the application/x-www-form-urlencoded content type
             */
            FORM_POST
        }

        /**
         * Authorization code flow response mode
         */
        @ConfigItem(defaultValueDocumentation = "query")
        public Optional<ResponseMode> responseMode = Optional.empty();

        /**
         * Relative path for calculating a "redirect_uri" query parameter.
         * It has to start from a forward slash and will be appended to the request URI's host and port.
         * For example, if the current request URI is 'https://localhost:8080/service' then a 'redirect_uri' parameter
         * will be set to 'https://localhost:8080/' if this property is set to '/' and be the same as the request URI
         * if this property has not been configured.
         * Note the original request URI will be restored after the user has authenticated if 'restorePathAfterRedirect' is set
         * to 'true'.
         */
        @ConfigItem
        public Optional<String> redirectPath = Optional.empty();

        /**
         * If this property is set to 'true' then the original request URI which was used before
         * the authentication will be restored after the user has been redirected back to the application.
         *
         * Note if `redirectPath` property is not set, the original request URI will be restored even if this property is
         * disabled.
         */
        @ConfigItem(defaultValue = "false")
        public boolean restorePathAfterRedirect;

        /**
         * Remove the query parameters such as 'code' and 'state' set by the OIDC server on the redirect URI
         * after the user has authenticated by redirecting a user to the same URI but without the query parameters.
         */
        @ConfigItem(defaultValue = "true")
        public boolean removeRedirectParameters = true;

        /**
         * Relative path to the public endpoint which will process the error response from the OIDC authorization endpoint.
         * If the user authentication has failed then the OIDC provider will return an 'error' and an optional
         * 'error_description'
         * parameters, instead of the expected authorization 'code'.
         *
         * If this property is set then the user will be redirected to the endpoint which can return a user-friendly
         * error description page. It has to start from a forward slash and will be appended to the request URI's host and port.
         * For example, if it is set as '/error' and the current request URI is
         * 'https://localhost:8080/callback?error=invalid_scope'
         * then a redirect will be made to 'https://localhost:8080/error?error=invalid_scope'.
         *
         * If this property is not set then HTTP 401 status will be returned in case of the user authentication failure.
         */
        @ConfigItem
        public Optional<String> errorPath = Optional.empty();

        /**
         * Both ID and access tokens are fetched from the OIDC provider as part of the authorization code flow.
         * ID token is always verified on every user request as the primary token which is used
         * to represent the principal and extract the roles.
         * Access token is not verified by default since it is meant to be propagated to the downstream services.
         * The verification of the access token should be enabled if it is injected as a JWT token.
         *
         * Access tokens obtained as part of the code flow will always be verified if `quarkus.oidc.roles.source`
         * property is set to `accesstoken` which means the authorization decision will be based on the roles extracted from the
         * access token.
         *
         * Bearer access tokens are always verified.
         */
        @ConfigItem(defaultValue = "false")
        public boolean verifyAccessToken;

        /**
         * Force 'https' as the 'redirect_uri' parameter scheme when running behind an SSL terminating reverse proxy.
         * This property, if enabled, will also affect the logout `post_logout_redirect_uri` and the local redirect requests.
         */
        @ConfigItem(defaultValueDocumentation = "false")
        public Optional<Boolean> forceRedirectHttpsScheme = Optional.empty();

        /**
         * List of scopes
         */
        @ConfigItem
        public Optional<List<String>> scopes = Optional.empty();

        /**
         * Add the 'openid' scope automatically to the list of scopes. This is required for OpenId Connect providers
         * but will not work for OAuth2 providers such as Twitter OAuth2 which does not accept that scope and throws an error.
         */
        @ConfigItem(defaultValueDocumentation = "true")
        public Optional<Boolean> addOpenidScope = Optional.empty();

        /**
         * Additional properties which will be added as the query parameters to the authentication redirect URI.
         */
        @ConfigItem
        public Map<String, String> extraParams = new HashMap<>();

        /**
         * Request URL query parameters which, if present, will be added to the authentication redirect URI.
         */
        @ConfigItem
        @ConvertWith(TrimmedStringConverter.class)
        public Optional<List<String>> forwardParams = Optional.empty();

        /**
         * If enabled the state, session and post logout cookies will have their 'secure' parameter set to 'true'
         * when HTTP is used. It may be necessary when running behind an SSL terminating reverse proxy.
         * The cookies will always be secure if HTTPS is used even if this property is set to false.
         */
        @ConfigItem(defaultValue = "false")
        public boolean cookieForceSecure;

        /**
         * Cookie name suffix.
         * For example, a session cookie name for the default OIDC tenant is 'q_session' but can be changed to 'q_session_test'
         * if this property is set to 'test'.
         */
        @ConfigItem
        public Optional<String> cookieSuffix = Optional.empty();

        /**
         * Cookie path parameter value which, if set, will be used to set a path parameter for the session, state and post
         * logout cookies.
         * The `cookie-path-header` property, if set, will be checked first.
         */
        @ConfigItem(defaultValue = "/")
        public String cookiePath = "/";

        /**
         * Cookie path header parameter value which, if set, identifies the incoming HTTP header
         * whose value will be used to set a path parameter for the session, state and post logout cookies.
         * If the header is missing then the `cookie-path` property will be checked.
         */
        @ConfigItem
        public Optional<String> cookiePathHeader = Optional.empty();

        /**
         * Cookie domain parameter value which, if set, will be used for the session, state and post logout cookies.
         */
        @ConfigItem
        public Optional<String> cookieDomain = Optional.empty();

        /**
         * If this property is set to 'true' then an OIDC UserInfo endpoint will be called.
         */
        @ConfigItem(defaultValueDocumentation = "false")
        public Optional<Boolean> userInfoRequired = Optional.empty();

        /**
         * Session age extension in minutes.
         * The user session age property is set to the value of the ID token life-span by default and
         * the user will be redirected to the OIDC provider to re-authenticate once the session has expired.
         * If this property is set to a non-zero value then the expired ID token can be refreshed before
         * the session has expired.
         * This property will be ignored if the `token.refresh-expired` property has not been enabled.
         */
        @ConfigItem(defaultValue = "5M")
        public Duration sessionAgeExtension = Duration.ofMinutes(5);

        /**
         * If this property is set to 'true' then a normal 302 redirect response will be returned
         * if the request was initiated via JavaScript API such as XMLHttpRequest or Fetch and the current user needs to be
         * (re)authenticated which may not be desirable for Single Page Applications since
         * it automatically following the redirect may not work given that OIDC authorization endpoints typically do not support
         * CORS.
         * If this property is set to `false` then a status code of '499' will be returned to allow
         * the client to handle the redirect manually
         */
        @ConfigItem(defaultValue = "true")
        public boolean javaScriptAutoRedirect = true;

        /**
         * Requires that ID token is available when the authorization code flow completes.
         * Disable this property only when you need to use the authorization code flow with OAuth2 providers which do not return
         * ID token - an internal IdToken will be generated in such cases.
         */
        @ConfigItem(defaultValueDocumentation = "true")
        public Optional<Boolean> idTokenRequired = Optional.empty();

        /**
         * Requires that a Proof Key for Code Exchange (PKCE) is used.
         */
        @ConfigItem(defaultValueDocumentation = "false")
        public Optional<Boolean> pkceRequired = Optional.empty();

        /**
         * Secret which will be used to encrypt a Proof Key for Code Exchange (PKCE) code verifier in the code flow state.
         * This secret must be set if PKCE is required but no client secret is set.
         * The length of the secret which will be used to encrypt the code verifier must be 32 characters long.
         */
        @ConfigItem
        public Optional<String> pkceSecret = Optional.empty();

        public Optional<Boolean> isPkceRequired() {
            return pkceRequired;
        }

        public void setPkceRequired(boolean pkceRequired) {
            this.pkceRequired = Optional.of(pkceRequired);
        }

        public Optional<String> getPkceSecret() {
            return pkceSecret;
        }

        public void setPkceSecret(String pkceSecret) {
            this.pkceSecret = Optional.of(pkceSecret);
        }

        public Optional<String> getErrorPath() {
            return errorPath;
        }

        public void setErrorPath(String errorPath) {
            this.errorPath = Optional.of(errorPath);
        }

        public boolean isJavaScriptAutoRedirect() {
            return javaScriptAutoRedirect;
        }

        public void setJavaScriptAutoredirect(boolean autoRedirect) {
            this.javaScriptAutoRedirect = autoRedirect;
        }

        public Optional<String> getRedirectPath() {
            return redirectPath;
        }

        public void setRedirectPath(String redirectPath) {
            this.redirectPath = Optional.of(redirectPath);
        }

        public Optional<List<String>> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = Optional.of(scopes);
        }

        public Map<String, String> getExtraParams() {
            return extraParams;
        }

        public void setExtraParams(Map<String, String> extraParams) {
            this.extraParams = extraParams;
        }

        public void setAddOpenidScope(boolean addOpenidScope) {
            this.addOpenidScope = Optional.of(addOpenidScope);
        }

        public Optional<Boolean> isAddOpenidScope() {
            return addOpenidScope;
        }

        public Optional<Boolean> isForceRedirectHttpsScheme() {
            return forceRedirectHttpsScheme;
        }

        public void setForceRedirectHttpsScheme(boolean forceRedirectHttpsScheme) {
            this.forceRedirectHttpsScheme = Optional.of(forceRedirectHttpsScheme);
        }

        public boolean isRestorePathAfterRedirect() {
            return restorePathAfterRedirect;
        }

        public void setRestorePathAfterRedirect(boolean restorePathAfterRedirect) {
            this.restorePathAfterRedirect = restorePathAfterRedirect;
        }

        public boolean isCookieForceSecure() {
            return cookieForceSecure;
        }

        public void setCookieForceSecure(boolean cookieForceSecure) {
            this.cookieForceSecure = cookieForceSecure;
        }

        public String getCookiePath() {
            return cookiePath;
        }

        public void setCookiePath(String cookiePath) {
            this.cookiePath = cookiePath;
        }

        public Optional<String> getCookieDomain() {
            return cookieDomain;
        }

        public void setCookieDomain(String cookieDomain) {
            this.cookieDomain = Optional.of(cookieDomain);
        }

        public Optional<Boolean> isUserInfoRequired() {
            return userInfoRequired;
        }

        public void setUserInfoRequired(boolean userInfoRequired) {
            this.userInfoRequired = Optional.of(userInfoRequired);
        }

        public boolean isRemoveRedirectParameters() {
            return removeRedirectParameters;
        }

        public void setRemoveRedirectParameters(boolean removeRedirectParameters) {
            this.removeRedirectParameters = removeRedirectParameters;
        }

        public boolean isVerifyAccessToken() {
            return verifyAccessToken;
        }

        public void setVerifyAccessToken(boolean verifyAccessToken) {
            this.verifyAccessToken = verifyAccessToken;
        }

        public Duration getSessionAgeExtension() {
            return sessionAgeExtension;
        }

        public void setSessionAgeExtension(Duration sessionAgeExtension) {
            this.sessionAgeExtension = sessionAgeExtension;
        }

        public Optional<String> getCookiePathHeader() {
            return cookiePathHeader;
        }

        public void setCookiePathHeader(String cookiePathHeader) {
            this.cookiePathHeader = Optional.of(cookiePathHeader);
        }

        public Optional<Boolean> isIdTokenRequired() {
            return idTokenRequired;
        }

        public void setIdTokenRequired(boolean idTokenRequired) {
            this.idTokenRequired = Optional.of(idTokenRequired);
        }

        public Optional<String> getCookieSuffix() {
            return cookieSuffix;
        }

        public void setCookieSuffix(String cookieSuffix) {
            this.cookieSuffix = Optional.of(cookieSuffix);
        }

        public Optional<ResponseMode> getResponseMode() {
            return responseMode;
        }

        public void setResponseMode(ResponseMode responseMode) {
            this.responseMode = Optional.of(responseMode);
        }

        public Optional<List<String>> getForwardParams() {
            return forwardParams;
        }

        public void setForwardParams(List<String> forwardParams) {
            this.forwardParams = Optional.of(forwardParams);
        }
    }

    /**
     * Authorization Code grant configuration
     */
    @ConfigGroup
    public static class CodeGrant {

        /**
         * Additional parameters, in addition to the required `code` and `redirect-uri` parameters,
         * which have to be included to complete the authorization code grant request.
         */
        @ConfigItem
        public Map<String, String> extraParams = new HashMap<>();

        /**
         * Custom HTTP headers which have to be sent to complete the authorization code grant request.
         */
        @ConfigItem
        public Map<String, String> headers = new HashMap<>();

        public Map<String, String> getExtraParams() {
            return extraParams;
        }

        public void setExtraParams(Map<String, String> extraParams) {
            this.extraParams = extraParams;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
    }

    @ConfigGroup
    public static class Token {

        public static Token fromIssuer(String issuer) {
            Token tokenClaims = new Token();
            tokenClaims.issuer = Optional.of(issuer);
            tokenClaims.audience = Optional.ofNullable(null);
            return tokenClaims;
        }

        public static Token fromAudience(String... audience) {
            Token tokenClaims = new Token();
            tokenClaims.issuer = Optional.ofNullable(null);
            tokenClaims.audience = Optional.of(Arrays.asList(audience));
            return tokenClaims;
        }

        /**
         * Expected issuer 'iss' claim value.
         * Note this property overrides the `issuer` property which may be set in OpenId Connect provider's well-known
         * configuration.
         * If the `iss` claim value varies depending on the host/IP address or tenant id of the provider then you may skip the
         * issuer verification by setting this property to 'any' but it should be done only when other options (such as
         * configuring
         * the provider to use the fixed `iss` claim value) are not possible.
         */
        @ConfigItem
        public Optional<String> issuer = Optional.empty();

        /**
         * Expected audience 'aud' claim value which may be a string or an array of strings.
         */
        @ConfigItem
        public Optional<List<String>> audience = Optional.empty();

        /**
         * A map of required claims and their expected values.
         * For example, `quarkus.oidc.token.required-claims.org_id = org_xyz` would require tokens to have the `org_id` claim to
         * be present and set to `org_xyz`.
         * Strings are the only supported types. Use {@linkplain SecurityIdentityAugmentor} to verify claims of other types or
         * complex claims.
         */
        @ConfigItem
        @ConfigDocMapKey("claim-name")
        public Map<String, String> requiredClaims = new HashMap<>();

        /**
         * Expected token type
         */
        @ConfigItem
        public Optional<String> tokenType = Optional.empty();

        /**
         * Life span grace period in seconds.
         * When checking token expiry, current time is allowed to be later than token expiration time by at most the configured
         * number of seconds.
         * When checking token issuance, current time is allowed to be sooner than token issue time by at most the configured
         * number of seconds.
         */
        @ConfigItem
        public OptionalInt lifespanGrace = OptionalInt.empty();

        /**
         * Token age.
         *
         * It allows for the number of seconds to be specified that must not elapse since the `iat` (issued at) time.
         * A small leeway to account for clock skew which can be configured with 'quarkus.oidc.token.lifespan-grace' to verify
         * the token expiry time
         * can also be used to verify the token age property.
         *
         * Note that setting this property does not relax the requirement that Bearer and Code Flow JWT tokens
         * must have a valid ('exp') expiry claim value. The only exception where setting this property relaxes the requirement
         * is when a logout token is sent with a back-channel logout request since the current
         * OpenId Connect Back-Channel specification does not explicitly require the logout tokens to contain an 'exp' claim.
         * However, even if the current logout token is allowed to have no 'exp' claim, the `exp` claim will be still verified
         * if the logout token contains it.
         */
        @ConfigItem
        public Optional<Duration> age = Optional.empty();

        /**
         * Name of the claim which contains a principal name. By default, the 'upn', 'preferred_username' and `sub` claims are
         * checked.
         */
        @ConfigItem
        public Optional<String> principalClaim = Optional.empty();

        /**
         * Refresh expired ID tokens.
         * If this property is enabled then a refresh token request will be performed if the ID token has expired
         * and, if successful, the local session will be updated with the new set of tokens.
         * Otherwise, the local session will be invalidated and the user redirected to the OpenID Provider to re-authenticate.
         * In this case the user may not be challenged again if the OIDC provider session is still active.
         *
         * For this option be effective the `authentication.session-age-extension` property should also be set to a non-zero
         * value since the refresh token is currently kept in the user session.
         *
         * This option is valid only when the application is of type {@link ApplicationType#WEB_APP}}.
         */
        @ConfigItem
        public boolean refreshExpired;

        /**
         * Refresh token time skew in seconds.
         * If this property is enabled then the configured number of seconds is added to the current time
         * when checking whether the access token should be refreshed. If the sum is greater than this access token's
         * expiration time then a refresh is going to happen.
         *
         * This property will be ignored if the 'refresh-expired' property is not enabled.
         */
        @ConfigItem
        public Optional<Duration> refreshTokenTimeSkew = Optional.empty();

        /**
         * Forced JWK set refresh interval in minutes.
         */
        @ConfigItem(defaultValue = "10M")
        public Duration forcedJwkRefreshInterval = Duration.ofMinutes(10);

        /**
         * Custom HTTP header that contains a bearer token.
         * This option is valid only when the application is of type {@link ApplicationType#SERVICE}}.
         */
        @ConfigItem
        public Optional<String> header = Optional.empty();

        /**
         * Decryption key location.
         * JWT tokens can be inner-signed and encrypted by OpenId Connect providers.
         * However, it is not always possible to remotely introspect such tokens because
         * the providers may not control the private decryption keys.
         * In such cases set this property to point to the file containing the decryption private key in
         * PEM or JSON Web Key (JWK) format.
         * Note that if a 'private_key_jwt' client authentication method is used then the private key
         * which is used to sign client authentication JWT tokens will be used to try to decrypt an encrypted ID token
         * if this property is not set.
         */
        @ConfigItem
        public Optional<String> decryptionKeyLocation = Optional.empty();

        /**
         * Allow the remote introspection of JWT tokens when no matching JWK key is available.
         *
         * Note this property is set to 'true' by default for backward-compatibility reasons and will be set to `false`
         * instead in one of the next releases.
         *
         * Also note this property will be ignored if JWK endpoint URI is not available and introspecting the tokens is
         * the only verification option.
         */
        @ConfigItem(defaultValue = "true")
        public boolean allowJwtIntrospection = true;

        /**
         * Require that JWT tokens are only introspected remotely.
         *
         */
        @ConfigItem(defaultValue = "false")
        public boolean requireJwtIntrospectionOnly = false;

        /**
         * Allow the remote introspection of the opaque tokens.
         *
         * Set this property to 'false' if only JWT tokens are expected.
         */
        @ConfigItem(defaultValue = "true")
        public boolean allowOpaqueTokenIntrospection = true;

        public Optional<String> getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = Optional.of(issuer);
        }

        public Optional<String> getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = Optional.of(header);
        }

        public Optional<List<String>> getAudience() {
            return audience;
        }

        public void setAudience(List<String> audience) {
            this.audience = Optional.of(audience);
        }

        public OptionalInt getLifespanGrace() {
            return lifespanGrace;
        }

        public void setLifespanGrace(int lifespanGrace) {
            this.lifespanGrace = OptionalInt.of(lifespanGrace);
        }

        public Optional<String> getPrincipalClaim() {
            return principalClaim;
        }

        public void setPrincipalClaim(String principalClaim) {
            this.principalClaim = Optional.of(principalClaim);
        }

        public boolean isRefreshExpired() {
            return refreshExpired;
        }

        public void setRefreshExpired(boolean refreshExpired) {
            this.refreshExpired = refreshExpired;
        }

        public Duration getForcedJwkRefreshInterval() {
            return forcedJwkRefreshInterval;
        }

        public void setForcedJwkRefreshInterval(Duration forcedJwkRefreshInterval) {
            this.forcedJwkRefreshInterval = forcedJwkRefreshInterval;
        }

        public Optional<String> getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = Optional.of(tokenType);
        }

        public Optional<Duration> getRefreshTokenTimeSkew() {
            return refreshTokenTimeSkew;
        }

        public void setRefreshTokenTimeSkew(Duration refreshTokenTimeSkew) {
            this.refreshTokenTimeSkew = Optional.of(refreshTokenTimeSkew);
        }

        public boolean isAllowJwtIntrospection() {
            return allowJwtIntrospection;
        }

        public void setAllowJwtIntrospection(boolean allowJwtIntrospection) {
            this.allowJwtIntrospection = allowJwtIntrospection;
        }

        public boolean isAllowOpaqueTokenIntrospection() {
            return allowOpaqueTokenIntrospection;
        }

        public void setAllowOpaqueTokenIntrospection(boolean allowOpaqueTokenIntrospection) {
            this.allowOpaqueTokenIntrospection = allowOpaqueTokenIntrospection;
        }

        public Optional<Duration> getAge() {
            return age;
        }

        public void setAge(Duration age) {
            this.age = Optional.of(age);
        }

        public Optional<String> getDecryptionKeyLocation() {
            return decryptionKeyLocation;
        }

        public void setDecryptionKeyLocation(String decryptionKeyLocation) {
            this.decryptionKeyLocation = Optional.of(decryptionKeyLocation);
        }

        public Map<String, String> getRequiredClaims() {
            return requiredClaims;
        }

        public void setRequiredClaims(Map<String, String> requiredClaims) {
            this.requiredClaims = requiredClaims;
        }

        public boolean isRequireJwtIntrospectionOnly() {
            return requireJwtIntrospectionOnly;
        }

        public void setRequireJwtIntrospectionOnly(boolean requireJwtIntrospectionOnly) {
            this.requireJwtIntrospectionOnly = requireJwtIntrospectionOnly;
        }
    }

    public static enum ApplicationType {
        /**
         * A {@code WEB_APP} is a client that serves pages, usually a frontend application. For this type of client the
         * Authorization Code Flow is defined as the preferred method for authenticating users.
         */
        WEB_APP,

        /**
         * A {@code SERVICE} is a client that has a set of protected HTTP resources, usually a backend application following the
         * RESTful Architectural Design. For this type of client, the Bearer Authorization method is defined as the preferred
         * method for authenticating and authorizing users.
         */
        SERVICE,

        /**
         * A combined {@code SERVICE} and {@code WEB_APP} client.
         * For this type of client, the Bearer Authorization method will be used if the Authorization header is set
         * and Authorization Code Flow - if not.
         */
        HYBRID
    }

    /**
     * Well known OpenId Connect provider identifier
     */
    @ConfigItem
    public Optional<Provider> provider = Optional.empty();

    public static enum Provider {
        APPLE,
        FACEBOOK,
        GITHUB,
        GOOGLE,
        MICROSOFT,
        SPOTIFY,
        TWITTER
    }

    public Optional<Provider> getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = Optional.of(provider);
    }

    public Optional<ApplicationType> getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(ApplicationType type) {
        this.applicationType = Optional.of(type);
    }

    public boolean isAllowTokenIntrospectionCache() {
        return allowTokenIntrospectionCache;
    }

    public void setAllowTokenIntrospectionCache(boolean allowTokenIntrospectionCache) {
        this.allowTokenIntrospectionCache = allowTokenIntrospectionCache;
    }

    public boolean isAllowUserInfoCache() {
        return allowUserInfoCache;
    }

    public void setAllowUserInfoCache(boolean allowUserInfoCache) {
        this.allowUserInfoCache = allowUserInfoCache;
    }

    public boolean isCacheUserInfoInIdtoken() {
        return cacheUserInfoInIdtoken;
    }

    public void setCacheUserInfoInIdtoken(boolean cacheUserInfoInIdtoken) {
        this.cacheUserInfoInIdtoken = cacheUserInfoInIdtoken;
    }

    public IntrospectionCredentials getIntrospectionCredentials() {
        return introspectionCredentials;
    }

    public void setIntrospectionCredentials(IntrospectionCredentials introspectionCredentials) {
        this.introspectionCredentials = introspectionCredentials;
    }

    public CodeGrant getCodeGrant() {
        return codeGrant;
    }

    public void setCodeGrant(CodeGrant codeGrant) {
        this.codeGrant = codeGrant;
    }
}
