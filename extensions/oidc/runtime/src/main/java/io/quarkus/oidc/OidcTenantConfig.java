package io.quarkus.oidc;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

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
     * A unique tenant identifier. It can be set by {@code TenantConfigResolver} providers, which
     * resolve the tenant configuration dynamically.
     */
    @ConfigItem
    public Optional<String> tenantId = Optional.empty();

    /**
     * If this tenant configuration is enabled.
     *
     * The default tenant is disabled if it is not configured but
     * a {@link TenantConfigResolver} that resolves tenant configurations is registered,
     * or named tenants are configured.
     * In this case, you do not need to disable the default tenant.
     */
    @ConfigItem(defaultValue = "true")
    public boolean tenantEnabled = true;

    /**
     * The application type, which can be one of the following {@link ApplicationType} values.
     */
    @ConfigItem(defaultValueDocumentation = "service")
    public Optional<ApplicationType> applicationType = Optional.empty();

    /**
     * The relative path or absolute URL of the OpenID Connect (OIDC) authorization endpoint, which authenticates
     * users.
     * You must set this property for `web-app` applications if OIDC discovery is disabled.
     * This property is ignored if OIDC discovery is enabled.
     */
    @ConfigItem
    public Optional<String> authorizationPath = Optional.empty();

    /**
     * The relative path or absolute URL of the OIDC UserInfo endpoint.
     * You must set this property for `web-app` applications if OIDC discovery is disabled
     * and the `authentication.user-info-required` property is enabled.
     * This property is ignored if OIDC discovery is enabled.
     */
    @ConfigItem
    public Optional<String> userInfoPath = Optional.empty();

    /**
     * Relative path or absolute URL of the OIDC RFC7662 introspection endpoint which can introspect both opaque and
     * JSON Web Token (JWT) tokens.
     * This property must be set if OIDC discovery is disabled and 1) the opaque bearer access tokens must be verified
     * or 2) JWT tokens must be verified while the cached JWK verification set with no matching JWK is being refreshed.
     * This property is ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> introspectionPath = Optional.empty();

    /**
     * Relative path or absolute URL of the OIDC JSON Web Key Set (JWKS) endpoint which returns a JSON Web Key
     * Verification Set.
     * This property should be set if OIDC discovery is disabled and the local JWT verification is required.
     * This property is ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> jwksPath = Optional.empty();

    /**
     * Relative path or absolute URL of the OIDC end_session_endpoint.
     * This property must be set if OIDC discovery is disabled and RP Initiated Logout support for the `web-app` applications is
     * required.
     * This property is ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> endSessionPath = Optional.empty();

    /**
     * The public key for the local JWT token verification.
     * OIDC server connection is not created when this property is set.
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
         * Include OpenId Connect Client ID configured with `quarkus.oidc.client-id`.
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
     * Configuration of the certificate chain which can be used to verify tokens.
     * If the certificate chain trusstore is configured, the tokens can be verified using the certificate
     * chain inlined in the Base64-encoded format as an `x5c` header in the token itself.
     */
    @ConfigItem
    public CertificateChain certificateChain = new CertificateChain();

    @ConfigGroup
    public static class CertificateChain {
        /**
         * Truststore file which keeps thumbprints of the trusted certificates.
         */
        @ConfigItem
        public Optional<Path> trustStoreFile = Optional.empty();

        /**
         * A parameter to specify the password of the truststore file if it is configured with {@link #trustStoreFile}.
         */
        @ConfigItem
        public Optional<String> trustStorePassword;

        /**
         * A parameter to specify the alias of the truststore certificate.
         */
        @ConfigItem
        public Optional<String> trustStoreCertAlias = Optional.empty();

        /**
         * An optional parameter to specify type of the truststore file. If not given, the type is automatically
         * detected
         * based on the file name.
         */
        @ConfigItem
        public Optional<String> trustStoreFileType = Optional.empty();

        public Optional<Path> getTrustStoreFile() {
            return trustStoreFile;
        }

        public void setTrustStoreFile(Path trustStoreFile) {
            this.trustStoreFile = Optional.of(trustStoreFile);
        }

        public Optional<String> getTrustStoreCertAlias() {
            return trustStoreCertAlias;
        }

        public void setTrustStoreCertAlias(String trustStoreCertAlias) {
            this.trustStoreCertAlias = Optional.of(trustStoreCertAlias);
        }

        public Optional<String> getTrustStoreFileType() {
            return trustStoreFileType;
        }

        public void setTrustStoreFileType(Optional<String> trustStoreFileType) {
            this.trustStoreFileType = trustStoreFileType;
        }
    }

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
     * for a given tenant. If the default token cache can be used, see {@link OidcConfig.TokenCache} to enable
     * it.
     */
    @ConfigItem(defaultValue = "true")
    public boolean allowTokenIntrospectionCache = true;

    /**
     * Allow caching the user info data.
     * Note enabling this property does not enable the cache itself but only permits to cache the user info data
     * for a given tenant. If the default token cache can be used, see {@link OidcConfig.TokenCache} to enable
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
         * The relative path of the logout endpoint at the application. If provided, the application is able to
         * initiate the
         * logout through this endpoint in conformance with the OpenID Connect RP-Initiated Logout specification.
         */
        @ConfigItem
        public Optional<String> path = Optional.empty();

        /**
         * Relative path of the application endpoint where the user should be redirected to after logging out from the
         * OpenID
         * Connect Provider.
         * This endpoint URI must be properly registered at the OpenID Connect Provider as a valid redirect URI.
         */
        @ConfigItem
        public Optional<String> postLogoutPath = Optional.empty();

        /**
         * Name of the post logout URI parameter which is added as a query parameter to the logout redirect URI.
         */
        @ConfigItem(defaultValue = OidcConstants.POST_LOGOUT_REDIRECT_URI)
        public String postLogoutUriParam;

        /**
         * Additional properties which is added as the query parameters to the logout redirect URI.
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

        public Optional<String> getPath() {
            return path;
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

        /**
         * Maximum number of logout tokens that can be cached before they are matched against ID tokens stored in session
         * cookies.
         */
        @ConfigItem(defaultValue = "10")
        public int tokenCacheSize = 10;

        /**
         * Number of minutes a logout token can be cached for.
         */
        @ConfigItem(defaultValue = "10M")
        public Duration tokenCacheTimeToLive = Duration.ofMinutes(10);

        /**
         * Token cache timer interval.
         * If this property is set, a timer checks and removes the stale entries periodically.
         */
        @ConfigItem
        public Optional<Duration> cleanUpTimerInterval = Optional.empty();

        /**
         * Logout token claim whose value is used as a key for caching the tokens.
         * Only `sub` (subject) and `sid` (session id) claims can be used as keys.
         * Set it to `sid` only if ID tokens issued by the OIDC provider have no `sub` but have `sid` claim.
         */
        @ConfigItem(defaultValue = "sub")
        public String logoutTokenKey = "sub";

        public void setPath(Optional<String> path) {
            this.path = path;
        }

        public Optional<String> getPath() {
            return path;
        }

        public String getLogoutTokenKey() {
            return logoutTokenKey;
        }

        public void setLogoutTokenKey(String logoutTokenKey) {
            this.logoutTokenKey = logoutTokenKey;
        }

        public int getTokenCacheSize() {
            return tokenCacheSize;
        }

        public void setTokenCacheSize(int tokenCacheSize) {
            this.tokenCacheSize = tokenCacheSize;
        }

        public Duration getTokenCacheTimeToLive() {
            return tokenCacheTimeToLive;
        }

        public void setTokenCacheTimeToLive(Duration tokenCacheTimeToLive) {
            this.tokenCacheTimeToLive = tokenCacheTimeToLive;
        }

        public Optional<Duration> getCleanUpTimerInterval() {
            return cleanUpTimerInterval;
        }

        public void setCleanUpTimerInterval(Duration cleanUpTimerInterval) {
            this.cleanUpTimerInterval = Optional.of(cleanUpTimerInterval);
        }
    }

    /**
     * Configuration for controlling how JsonWebKeySet containing verification keys should be acquired and managed.
     */
    @ConfigItem
    public Jwks jwks = new Jwks();

    @ConfigGroup
    public static class Jwks {

        /**
         * If JWK verification keys should be fetched at the moment a connection to the OIDC provider
         * is initialized.
         * <p/>
         * Disabling this property delays the key acquisition until the moment the current token
         * has to be verified. Typically it can only be necessary if the token or other telated request properties
         * provide an additional context which is required to resolve the keys correctly.
         */
        @ConfigItem(defaultValue = "true")
        public boolean resolveEarly = true;

        /**
         * Maximum number of JWK keys that can be cached.
         * This property is ignored if the {@link #resolveEarly} property is set to true.
         */
        @ConfigItem(defaultValue = "10")
        public int cacheSize = 10;

        /**
         * Number of minutes a JWK key can be cached for.
         * This property is ignored if the {@link #resolveEarly} property is set to true.
         */
        @ConfigItem(defaultValue = "10M")
        public Duration cacheTimeToLive = Duration.ofMinutes(10);

        /**
         * Cache timer interval.
         * If this property is set, a timer checks and removes the stale entries periodically.
         * This property is ignored if the {@link #resolveEarly} property is set to true.
         */
        @ConfigItem
        public Optional<Duration> cleanUpTimerInterval = Optional.empty();

        public int getCacheSize() {
            return cacheSize;
        }

        public void setCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
        }

        public Duration getCacheTimeToLive() {
            return cacheTimeToLive;
        }

        public void setCacheTimeToLive(Duration cacheTimeToLive) {
            this.cacheTimeToLive = cacheTimeToLive;
        }

        public Optional<Duration> getCleanUpTimerInterval() {
            return cleanUpTimerInterval;
        }

        public void setCleanUpTimerInterval(Duration cleanUpTimerInterval) {
            this.cleanUpTimerInterval = Optional.of(cleanUpTimerInterval);
        }

        public boolean isResolveEarly() {
            return resolveEarly;
        }

        public void setResolveEarly(boolean resolveEarly) {
            this.resolveEarly = resolveEarly;
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

        public Optional<String> getPath() {
            return path;
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
         * Mandates that the Default TokenStateManager encrypt the session cookie that stores the tokens.
         */
        @ConfigItem(defaultValue = "true")
        public boolean encryptionRequired = true;

        /**
         * The secret used by the Default TokenStateManager to encrypt the session cookie
         * storing the tokens when {@link #encryptionRequired} property is enabled.
         * <p>
         * If this secret is not set, the client secret configured with
         * either `quarkus.oidc.credentials.secret` or `quarkus.oidc.credentials.client-secret.value` is checked.
         * Finally, `quarkus.oidc.credentials.jwt.secret` which can be used for `client_jwt_secret` authentication is
         * checked.
         * The secret is auto-generated if it remains uninitialized after checking all of these properties.
         * <p>
         * The length of the secret used to encrypt the tokens should be at least 32 characters long.
         * A warning is logged if the secret length is less than 16 characters.
         */
        @ConfigItem
        public Optional<String> encryptionSecret = Optional.empty();

        public boolean isEncryptionRequired() {
            return encryptionRequired;
        }

        public void setEncryptionRequired(boolean encryptionRequired) {
            this.encryptionRequired = encryptionRequired;
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
         * A list of paths to claims containing an array of groups.
         * Each path starts from the top level JWT JSON object
         * and can contain multiple segments.
         * Each segment represents a JSON object name only; for example: "realm/groups".
         * Use double quotes with the namespace-qualified claim names.
         * This property can be used if a token has no `groups` claim but has the groups set in one or more different claims.
         */
        @ConfigItem
        public Optional<List<String>> roleClaimPath = Optional.empty();
        /**
         * The separator for splitting strings that contain multiple group values.
         * It is only used if the "role-claim-path" property points to one or more custom claims whose values are strings.
         * A single space is used by default because the standard `scope` claim can contain a space-separated sequence.
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
             * ID Token - the default value for the `web-app` applications.
             */
            idtoken,

            /**
             * Access Token - the default value for the `service` applications;
             * can also be used as the source of roles for the `web-app` applications.
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
         * SameSite attribute values for the session cookie.
         */
        public enum CookieSameSite {
            STRICT,
            LAX,
            NONE
        }

        /**
         * Authorization code flow response mode
         */
        public enum ResponseMode {
            /**
             * Authorization response parameters are encoded in the query string added to the `redirect_uri`
             */
            QUERY,

            /**
             * Authorization response parameters are encoded as HTML form values that are auto-submitted in the browser
             * and transmitted by the HTTP POST method using the application/x-www-form-urlencoded content type
             */
            FORM_POST
        }

        /**
         * Authorization code flow response mode.
         */
        @ConfigItem(defaultValueDocumentation = "query")
        public Optional<ResponseMode> responseMode = Optional.empty();

        /**
         * The relative path for calculating a `redirect_uri` query parameter.
         * It has to start from a forward slash and is appended to the request URI's host and port.
         * For example, if the current request URI is `https://localhost:8080/service`, a `redirect_uri` parameter
         * is set to `https://localhost:8080/` if this property is set to `/` and be the same as the request URI
         * if this property has not been configured.
         * Note the original request URI is restored after the user has authenticated if `restorePathAfterRedirect` is set
         * to `true`.
         */
        @ConfigItem
        public Optional<String> redirectPath = Optional.empty();

        /**
         * If this property is set to `true`, the original request URI which was used before
         * the authentication is restored after the user has been redirected back to the application.
         *
         * Note if `redirectPath` property is not set, the original request URI is restored even if this property is
         * disabled.
         */
        @ConfigItem(defaultValue = "false")
        public boolean restorePathAfterRedirect;

        /**
         * Remove the query parameters such as `code` and `state` set by the OIDC server on the redirect URI
         * after the user has authenticated by redirecting a user to the same URI but without the query parameters.
         */
        @ConfigItem(defaultValue = "true")
        public boolean removeRedirectParameters = true;

        /**
         * Relative path to the public endpoint which processes the error response from the OIDC authorization
         * endpoint.
         * If the user authentication has failed, the OIDC provider returns an `error` and an optional
         * `error_description`
         * parameters, instead of the expected authorization `code`.
         *
         * If this property is set, the user is redirected to the endpoint which can return a user-friendly
         * error description page. It has to start from a forward slash and is appended to the request URI's host and port.
         * For example, if it is set as `/error` and the current request URI is
         * `https://localhost:8080/callback?error=invalid_scope`,
         * a redirect is made to `https://localhost:8080/error?error=invalid_scope`.
         *
         * If this property is not set, HTTP 401 status is returned in case of the user authentication failure.
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
         * Access tokens obtained as part of the code flow are always verified if `quarkus.oidc.roles.source`
         * property is set to `accesstoken` which means the authorization decision is based on the roles extracted from the
         * access token.
         *
         * Bearer access tokens are always verified.
         */
        @ConfigItem(defaultValue = "false")
        public boolean verifyAccessToken;

        /**
         * Force `https` as the `redirect_uri` parameter scheme when running behind an SSL/TLS terminating reverse
         * proxy.
         * This property, if enabled, also affects the logout `post_logout_redirect_uri` and the local redirect requests.
         */
        @ConfigItem(defaultValueDocumentation = "false")
        public Optional<Boolean> forceRedirectHttpsScheme = Optional.empty();

        /**
         * List of scopes
         */
        @ConfigItem
        public Optional<List<String>> scopes = Optional.empty();

        /**
         * Require that ID token includes a `nonce` claim which must match `nonce` authentication request query parameter.
         * Enabling this property can help mitigate replay attacks.
         * Do not enable this property if your OpenId Connect provider does not support setting `nonce` in ID token
         * or if you work with OAuth2 provider such as `GitHub` which does not issue ID tokens.
         */
        @ConfigItem(defaultValue = "false")
        public boolean nonceRequired = false;

        /**
         * Add the `openid` scope automatically to the list of scopes. This is required for OpenId Connect providers,
         * but does not work for OAuth2 providers such as Twitter OAuth2, which do not accept this scope and throw errors.
         */
        @ConfigItem(defaultValueDocumentation = "true")
        public Optional<Boolean> addOpenidScope = Optional.empty();

        /**
         * Additional properties added as query parameters to the authentication redirect URI.
         */
        @ConfigItem
        public Map<String, String> extraParams = new HashMap<>();

        /**
         * Request URL query parameters which, if present, are added to the authentication redirect URI.
         */
        @ConfigItem
        @ConvertWith(TrimmedStringConverter.class)
        public Optional<List<String>> forwardParams = Optional.empty();

        /**
         * If enabled the state, session, and post logout cookies have their `secure` parameter set to `true`
         * when HTTP is used. It might be necessary when running behind an SSL/TLS terminating reverse proxy.
         * The cookies are always secure if HTTPS is used, even if this property is set to false.
         */
        @ConfigItem(defaultValue = "false")
        public boolean cookieForceSecure;

        /**
         * Cookie name suffix.
         * For example, a session cookie name for the default OIDC tenant is `q_session` but can be changed to `q_session_test`
         * if this property is set to `test`.
         */
        @ConfigItem
        public Optional<String> cookieSuffix = Optional.empty();

        /**
         * Cookie path parameter value which, if set, is used to set a path parameter for the session, state and post
         * logout cookies.
         * The `cookie-path-header` property, if set, is checked first.
         */
        @ConfigItem(defaultValue = "/")
        public String cookiePath = "/";

        /**
         * Cookie path header parameter value which, if set, identifies the incoming HTTP header
         * whose value is used to set a path parameter for the session, state and post logout cookies.
         * If the header is missing, the `cookie-path` property is checked.
         */
        @ConfigItem
        public Optional<String> cookiePathHeader = Optional.empty();

        /**
         * Cookie domain parameter value which, if set, is used for the session, state and post logout cookies.
         */
        @ConfigItem
        public Optional<String> cookieDomain = Optional.empty();

        /**
         * SameSite attribute for the session cookie.
         */
        @ConfigItem(defaultValue = "lax")
        public CookieSameSite cookieSameSite = CookieSameSite.LAX;

        /**
         * If a state cookie is present, a `state` query parameter must also be present and both the state
         * cookie name suffix and state cookie value must match the value of the `state` query parameter when
         * the redirect path matches the current path.
         * However, if multiple authentications are attempted from the same browser, for example, from the different
         * browser tabs, then the currently available state cookie might represent the authentication flow
         * initiated from another tab and not related to the current request.
         * Disable this property to permit only a single authorization code flow in the same browser.
         *
         */
        @ConfigItem(defaultValue = "true")
        public boolean allowMultipleCodeFlows = true;

        /**
         * Fail with the HTTP 401 error if the state cookie is present but no state query parameter is present.
         * <p/>
         * When either multiple authentications are disabled or the redirect URL
         * matches the original request URL, the stale state cookie might remain in the browser cache from
         * the earlier failed redirect to an OpenId Connect provider and be visible during the current request.
         * For example, if Single-page application (SPA) uses XHR to handle redirects to the provider
         * which does not support CORS for its authorization endpoint, the browser blocks it
         * and the state cookie created by Quarkus remains in the browser cache.
         * Quarkus reports an authentication failure when it detects such an old state cookie but find no matching state
         * query parameter.
         * <p/>
         * Reporting HTTP 401 error is usually the right thing to do in such cases, it minimizes a risk of the
         * browser redirect loop but also can identify problems in the way SPA or Quarkus application manage redirects.
         * For example, enabling {@link #javaScriptAutoRedirect} or having the provider redirect to URL configured
         * with {@link #redirectPath} might be needed to avoid such errors.
         * <p/>
         * However, setting this property to `false` might help if the above options are not suitable.
         * It causes a new authentication redirect to OpenId Connect provider. Doing so might increase the
         * risk of browser redirect loops.
         */
        @ConfigItem(defaultValue = "false")
        public boolean failOnMissingStateParam = false;

        /**
         * If this property is set to `true`, an OIDC UserInfo endpoint is called.
         * This property is enabled if `quarkus.oidc.roles.source` is `userinfo`.
         * or `quarkus.oidc.token.verify-access-token-with-user-info` is `true`
         * or `quarkus.oidc.authentication.id-token-required` is set to `false`,
         * you do not need to enable this property manually in these cases.
         */
        @ConfigItem(defaultValueDocumentation = "false")
        public Optional<Boolean> userInfoRequired = Optional.empty();

        /**
         * Session age extension in minutes.
         * The user session age property is set to the value of the ID token life-span by default and
         * the user is redirected to the OIDC provider to re-authenticate once the session has expired.
         * If this property is set to a nonzero value, then the expired ID token can be refreshed before
         * the session has expired.
         * This property is ignored if the `token.refresh-expired` property has not been enabled.
         */
        @ConfigItem(defaultValue = "5M")
        public Duration sessionAgeExtension = Duration.ofMinutes(5);

        /**
         * If this property is set to `true`, a normal 302 redirect response is returned
         * if the request was initiated by a JavaScript API such as XMLHttpRequest or Fetch and the current user needs to be
         * (re)authenticated, which might not be desirable for Single-page applications (SPA) since
         * it automatically following the redirect might not work given that OIDC authorization endpoints typically do not
         * support
         * CORS.
         * <p/>
         * If this property is set to `false`, a status code of `499` is returned to allow
         * SPA to handle the redirect manually if a request header identifying current request as a JavaScript request is found.
         * `X-Requested-With` request header with its value set to either `JavaScript` or `XMLHttpRequest` is expected by
         * default if
         * this property is enabled. You can register a custom {@linkplain JavaScriptRequestChecker} to do a custom JavaScript
         * request check instead.
         */
        @ConfigItem(defaultValue = "true")
        public boolean javaScriptAutoRedirect = true;

        /**
         * Requires that ID token is available when the authorization code flow completes.
         * Disable this property only when you need to use the authorization code flow with OAuth2 providers which do not return
         * ID token - an internal IdToken is generated in such cases.
         */
        @ConfigItem(defaultValueDocumentation = "true")
        public Optional<Boolean> idTokenRequired = Optional.empty();

        /**
         * Internal ID token lifespan.
         * This property is only checked when an internal IdToken is generated when Oauth2 providers do not return IdToken.
         */
        @ConfigItem(defaultValueDocumentation = "5M")
        public Optional<Duration> internalIdTokenLifespan = Optional.empty();

        /**
         * Requires that a Proof Key for Code Exchange (PKCE) is used.
         */
        @ConfigItem(defaultValueDocumentation = "false")
        public Optional<Boolean> pkceRequired = Optional.empty();

        /**
         * Secret used to encrypt a Proof Key for Code Exchange (PKCE) code verifier in the code flow state.
         * This secret should be at least 32 characters long.
         *
         * @deprecated This field is deprecated. Use {@link #stateSecret} instead.
         *
         */
        @ConfigItem
        @Deprecated(forRemoval = true)
        public Optional<String> pkceSecret = Optional.empty();

        /**
         * Secret used to encrypt Proof Key for Code Exchange (PKCE) code verifier and/or nonce in the code flow
         * state.
         * This secret should be at least 32 characters long.
         * <p/>
         * If this secret is not set, the client secret configured with
         * either `quarkus.oidc.credentials.secret` or `quarkus.oidc.credentials.client-secret.value` is checked.
         * Finally, `quarkus.oidc.credentials.jwt.secret` which can be used for `client_jwt_secret` authentication is
         * checked. A client secret is not be used as a state encryption secret if it is less than 32 characters
         * long.
         * </p>
         * The secret is auto-generated if it remains uninitialized after checking all of these properties.
         * <p/>
         * Error is reported if the secret length is less than 16 characters.
         */
        @ConfigItem
        public Optional<String> stateSecret = Optional.empty();

        public Optional<Duration> getInternalIdTokenLifespan() {
            return internalIdTokenLifespan;
        }

        public void setInternalIdTokenLifespan(Duration internalIdTokenLifespan) {
            this.internalIdTokenLifespan = Optional.of(internalIdTokenLifespan);
        }

        public Optional<Boolean> isPkceRequired() {
            return pkceRequired;
        }

        public void setPkceRequired(boolean pkceRequired) {
            this.pkceRequired = Optional.of(pkceRequired);
        }

        @Deprecated(forRemoval = true)
        public Optional<String> getPkceSecret() {
            return pkceSecret;
        }

        @Deprecated(forRemoval = true)
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

        public CookieSameSite getCookieSameSite() {
            return cookieSameSite;
        }

        public void setCookieSameSite(CookieSameSite cookieSameSite) {
            this.cookieSameSite = cookieSameSite;
        }

        public boolean isAllowMultipleCodeFlows() {
            return allowMultipleCodeFlows;
        }

        public void setAllowMultipleCodeFlows(boolean allowMultipleCodeFlows) {
            this.allowMultipleCodeFlows = allowMultipleCodeFlows;
        }

        public boolean isNonceRequired() {
            return nonceRequired;
        }

        public void setNonceRequired(boolean nonceRequired) {
            this.nonceRequired = nonceRequired;
        }

        public Optional<String> getStateSecret() {
            return stateSecret;
        }

        public void setStateSecret(Optional<String> stateSecret) {
            this.stateSecret = stateSecret;
        }
    }

    /**
     * Authorization Code grant configuration
     */
    @ConfigGroup
    public static class CodeGrant {

        /**
         * Additional parameters, in addition to the required `code` and `redirect-uri` parameters,
         * which must be included to complete the authorization code grant request.
         */
        @ConfigItem
        public Map<String, String> extraParams = new HashMap<>();

        /**
         * Custom HTTP headers which must be sent to complete the authorization code grant request.
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

    /**
     * Supported asymmetric signature algorithms
     */
    public static enum SignatureAlgorithm {
        RS256,
        RS384,
        RS512,
        PS256,
        PS384,
        PS512,
        ES256,
        ES384,
        ES512,
        EDDSA;

        private static String EDDSA_ALG = "EDDSA";
        private static String REQUIRED_EDDSA_ALG = "EdDSA";

        public String getAlgorithm() {
            String name = name();
            return EDDSA_ALG.equals(name) ? REQUIRED_EDDSA_ALG : name;
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
         * The expected issuer `iss` claim value.
         * This property overrides the `issuer` property, which might be set in OpenId Connect provider's well-known
         * configuration.
         * If the `iss` claim value varies depending on the host, IP address, or tenant id of the provider, you can skip the
         * issuer verification by setting this property to `any`, but it should be done only when other options (such as
         * configuring
         * the provider to use the fixed `iss` claim value) are not possible.
         */
        @ConfigItem
        public Optional<String> issuer = Optional.empty();

        /**
         * The expected audience `aud` claim value, which can be a string or an array of strings.
         *
         * Note the audience claim is verified for ID tokens by default.
         * ID token audience must be equal to the value of `quarkus.oidc.client-id` property.
         * Use this property to override the expected value if your OpenID Connect provider
         * sets a different audience claim value in ID tokens. Set it to `any` if your provider
         * does not set ID token audience` claim.
         *
         * Audience verification for access tokens is only done if this property is configured.
         */
        @ConfigItem
        public Optional<List<String>> audience = Optional.empty();

        /**
         * Require that the token includes a `sub` (subject) claim which is a unique
         * and never reassigned identifier for the current user.
         * Note that if you enable this property and if UserInfo is also required,
         * both the token and UserInfo `sub` claims must be present and match each other.
         */
        @ConfigItem(defaultValue = "false")
        public boolean subjectRequired = false;

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
         * A small leeway to account for clock skew which can be configured with `quarkus.oidc.token.lifespan-grace` to verify
         * the token expiry time
         * can also be used to verify the token age property.
         *
         * Note that setting this property does not relax the requirement that Bearer and Code Flow JWT tokens
         * must have a valid (`exp`) expiry claim value. The only exception where setting this property relaxes the requirement
         * is when a logout token is sent with a back-channel logout request since the current
         * OpenId Connect Back-Channel specification does not explicitly require the logout tokens to contain an `exp` claim.
         * However, even if the current logout token is allowed to have no `exp` claim, the `exp` claim is still verified
         * if the logout token contains it.
         */
        @ConfigItem
        public Optional<Duration> age = Optional.empty();

        /**
         * Name of the claim which contains a principal name. By default, the `upn`, `preferred_username` and `sub`
         * claims are
         * checked.
         */
        @ConfigItem
        public Optional<String> principalClaim = Optional.empty();

        /**
         * Refresh expired authorization code flow ID or access tokens.
         * If this property is enabled, a refresh token request is performed if the authorization code
         * ID or access token has expired and, if successful, the local session is updated with the new set of tokens.
         * Otherwise, the local session is invalidated and the user redirected to the OpenID Provider to re-authenticate.
         * In this case, the user might not be challenged again if the OIDC provider session is still active.
         *
         * For this option be effective the `authentication.session-age-extension` property should also be set to a nonzero
         * value since the refresh token is currently kept in the user session.
         *
         * This option is valid only when the application is of type {@link ApplicationType#WEB_APP}}.
         *
         * This property is enabled if `quarkus.oidc.token.refresh-token-time-skew` is configured,
         * you do not need to enable this property manually in this case.
         */
        @ConfigItem
        public boolean refreshExpired;

        /**
         * The refresh token time skew, in seconds.
         * If this property is enabled, the configured number of seconds is added to the current time
         * when checking if the authorization code ID or access token should be refreshed.
         * If the sum is greater than the authorization code ID or access token's expiration time, a refresh is going to
         * happen.
         */
        @ConfigItem
        public Optional<Duration> refreshTokenTimeSkew = Optional.empty();

        /**
         * The forced JWK set refresh interval in minutes.
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
         * HTTP Authorization header scheme.
         */
        @ConfigItem(defaultValue = OidcConstants.BEARER_SCHEME)
        public String authorizationScheme = OidcConstants.BEARER_SCHEME;

        /**
         * Required signature algorithm.
         * OIDC providers support many signature algorithms but if necessary you can restrict
         * Quarkus application to accept tokens signed only using an algorithm configured with this property.
         */
        @ConfigItem
        public Optional<SignatureAlgorithm> signatureAlgorithm = Optional.empty();

        /**
         * Decryption key location.
         * JWT tokens can be inner-signed and encrypted by OpenId Connect providers.
         * However, it is not always possible to remotely introspect such tokens because
         * the providers might not control the private decryption keys.
         * In such cases set this property to point to the file containing the decryption private key in
         * PEM or JSON Web Key (JWK) format.
         * If this property is not set and the `private_key_jwt` client authentication method is used, the private key
         * used to sign the client authentication JWT tokens are also used to decrypt the encrypted ID tokens.
         */
        @ConfigItem
        public Optional<String> decryptionKeyLocation = Optional.empty();

        /**
         * Allow the remote introspection of JWT tokens when no matching JWK key is available.
         *
         * This property is set to `true` by default for backward-compatibility reasons. It is planned that this default value
         * will be changed to `false` in an upcoming release.
         *
         * Also note this property is ignored if JWK endpoint URI is not available and introspecting the tokens is
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
         * Set this property to `false` if only JWT tokens are expected.
         */
        @ConfigItem(defaultValue = "true")
        public boolean allowOpaqueTokenIntrospection = true;

        /**
         * Token customizer name.
         *
         * Allows to select a tenant specific token customizer as a named bean.
         * Prefer using {@link TenantFeature} qualifier when registering custom {@link TokenCustomizer}.
         * Use this property only to refer to `TokenCustomizer` implementations provided by this extension.
         */
        @ConfigItem
        public Optional<String> customizerName = Optional.empty();

        /**
         * Indirectly verify that the opaque (binary) access token is valid by using it to request UserInfo.
         * Opaque access token is considered valid if the provider accepted this token and returned a valid UserInfo.
         * You should only enable this option if the opaque access tokens must be accepted but OpenId Connect
         * provider does not have a token introspection endpoint.
         * This property has no effect when JWT tokens must be verified.
         */
        @ConfigItem(defaultValueDocumentation = "false")
        public Optional<Boolean> verifyAccessTokenWithUserInfo = Optional.empty();

        public Optional<Boolean> isVerifyAccessTokenWithUserInfo() {
            return verifyAccessTokenWithUserInfo;
        }

        public void setVerifyAccessTokenWithUserInfo(boolean verify) {
            this.verifyAccessTokenWithUserInfo = Optional.of(verify);
        }

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

        public Optional<SignatureAlgorithm> getSignatureAlgorithm() {
            return signatureAlgorithm;
        }

        public void setSignatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
            this.signatureAlgorithm = Optional.of(signatureAlgorithm);
        }

        public Optional<String> getCustomizerName() {
            return customizerName;
        }

        public void setCustomizerName(String customizerName) {
            this.customizerName = Optional.of(customizerName);
        }

        public boolean isSubjectRequired() {
            return subjectRequired;
        }

        public void setSubjectRequired(boolean subjectRequired) {
            this.subjectRequired = subjectRequired;
        }

        public String getAuthorizationScheme() {
            return authorizationScheme;
        }

        public void setAuthorizationScheme(String authorizationScheme) {
            this.authorizationScheme = authorizationScheme;
        }
    }

    public static enum ApplicationType {
        /**
         * A {@code WEB_APP} is a client that serves pages, usually a front-end application. For this type of client the
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
         * For this type of client, the Bearer Authorization method is used if the Authorization header is set
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
        DISCORD,
        FACEBOOK,
        GITHUB,
        GOOGLE,
        LINKEDIN,
        MASTODON,
        MICROSOFT,
        SPOTIFY,
        STRAVA,
        TWITCH,
        TWITTER,
        // New name for Twitter
        X
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

    public CertificateChain getCertificateChain() {
        return certificateChain;
    }

    public void setCertificateChain(CertificateChain certificateChain) {
        this.certificateChain = certificateChain;
    }
}
