package io.quarkus.oidc;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class OidcTenantConfig {

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
    @ConfigItem(defaultValue = "service")
    public ApplicationType applicationType = ApplicationType.SERVICE;

    /**
     * The base URL of the OpenID Connect (OIDC) server, for example, 'https://host:port/auth'.
     * OIDC discovery endpoint will be called by default by appending a '.well-known/openid-configuration' path to this URL.
     * Note if you work with Keycloak OIDC server, make sure the base URL is in the following format:
     * 'https://host:port/auth/realms/{realm}' where '{realm}' has to be replaced by the name of the Keycloak realm.
     */
    @ConfigItem
    public Optional<String> authServerUrl = Optional.empty();

    /**
     * Enables OIDC discovery.
     * If the discovery is disabled then the following properties must be configured:
     * - 'authorization-path' and 'token-path' for the 'web-app' applications
     * - 'jwks-path' or 'introspection-path' for both the 'web-app' and 'service' applications
     * <p>
     * 'web-app' applications may also have 'user-info-path' and 'end-session-path' properties configured.
     */
    @ConfigItem(defaultValue = "true")
    public boolean discoveryEnabled = true;

    /**
     * Relative path of the OIDC authorization endpoint which authenticates the users.
     * This property must be set for the 'web-app' applications if OIDC discovery is disabled.
     * This property will be ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> authorizationPath = Optional.empty();

    /**
     * Relative path of the OIDC token endpoint which issues ID, access and refresh tokens.
     * This property must be set for the 'web-app' applications if OIDC discovery is disabled.
     * This property will be ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> tokenPath = Optional.empty();

    /**
     * Relative path of the OIDC userinfo endpoint.
     * This property must only be set for the 'web-app' applications if OIDC discovery is disabled
     * and 'authentication.user-info-required' property is enabled.
     * This property will be ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> userInfoPath = Optional.empty();

    /**
     * Relative path of the OIDC RFC7662 introspection endpoint which can introspect both opaque and JWT tokens.
     * This property must be set if OIDC discovery is disabled and 1) the opaque bearer access tokens have to be verified
     * or 2) JWT tokens have to be verified while the cached JWK verification set with no matching JWK is being refreshed.
     * This property will be ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> introspectionPath = Optional.empty();

    /**
     * Relative path of the OIDC JWKS endpoint which returns a JSON Web Key Verification Set.
     * This property should be set if OIDC discovery is disabled and the local JWT verification is required.
     * This property will be ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> jwksPath = Optional.empty();

    /**
     * Relative path of the OIDC end_session_endpoint.
     * This property must be set if OIDC discovery is disabled and RP Initiated Logout support for the 'web-app' applications is
     * required.
     * This property will be ignored if the discovery is enabled.
     */
    @ConfigItem
    public Optional<String> endSessionPath = Optional.empty();

    /**
     * The maximum amount of time the adapter will try connecting to the currently unavailable OIDC server for.
     * For example, setting it to '20S' will let the adapter keep requesting the connection for up to 20 seconds.
     */
    @ConfigItem
    public Optional<Duration> connectionDelay = Optional.empty();

    /**
     * Public key for the local JWT token verification.
     * OIDC server connection will not be created when this property is set.
     */
    @ConfigItem
    public Optional<String> publicKey = Optional.empty();

    /**
     * The client-id of the application. Each application has a client-id that is used to identify the application
     */
    @ConfigItem
    public Optional<String> clientId = Optional.empty();

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
     * Credentials which the OIDC adapter will use to authenticate to the OIDC server.
     */
    @ConfigItem
    public Credentials credentials = new Credentials();

    /**
     * Options to configure a proxy that OIDC adapter will use for talking with OIDC server.
     */
    @ConfigItem
    public Proxy proxy = new Proxy();

    /**
     * Different options to configure authorization requests
     */
    public Authentication authentication = new Authentication();

    /**
     * TLS configurations
     */
    @ConfigItem
    public Tls tls = new Tls();

    /**
     * Logout configuration
     */
    @ConfigItem
    public Logout logout = new Logout();

    /**
     * Default token state manager configuration
     */
    @ConfigItem
    public TokenStateManager tokenStateManager = new TokenStateManager();

    @ConfigGroup
    public static class Tls {
        public enum Verification {
            /**
             * Certificates are validated and hostname verification is enabled. This is the default value.
             */
            REQUIRED,
            /**
             * All certificated are trusted and hostname verification is disabled.
             */
            NONE
        }

        /**
         * Certificate validation and hostname verification, which can be one of the following values from enum
         * {@link Verification}. Default is required.
         */
        public Optional<Verification> verification = Optional.empty();

        public Optional<Verification> getVerification() {
            return verification;
        }

        public void setVerification(Verification verification) {
            this.verification = Optional.ofNullable(verification);
        }

    }

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
            ID_TOKEN
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

        public boolean isSplitTokens() {
            return splitTokens;
        }

        public void setSplitTokens(boolean spliTokens) {
            this.splitTokens = spliTokens;
        }

        public Strategy getStrategy() {
            return strategy;
        }

        public void setStrategy(Strategy strategy) {
            this.strategy = strategy;
        }
    }

    public Optional<Duration> getConnectionDelay() {
        return connectionDelay;
    }

    public void setConnectionDelay(Duration connectionDelay) {
        this.connectionDelay = Optional.of(connectionDelay);
    }

    public Optional<String> getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = Optional.of(authServerUrl);
    }

    public Optional<String> getAuthorizationPath() {
        return authorizationPath;
    }

    public void setAuthorizationPath(String authorizationPath) {
        this.authorizationPath = Optional.of(authorizationPath);
    }

    public Optional<String> getTokenPath() {
        return tokenPath;
    }

    public void setTokenPath(String tokenPath) {
        this.tokenPath = Optional.of(tokenPath);
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

    public Optional<String> getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = Optional.of(clientId);
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

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
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

    public boolean isDiscoveryEnabled() {
        return discoveryEnabled;
    }

    public void setDiscoveryEnabled(boolean enabled) {
        this.discoveryEnabled = enabled;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public void setLogout(Logout logout) {
        this.logout = logout;
    }

    public Logout getLogout() {
        return logout;
    }

    @ConfigGroup
    public static class Credentials {

        /**
         * Client secret which is used for a 'client_secret_basic' authentication method.
         * Note that a 'client-secret.value' can be used instead but both properties are mutually exclusive.
         */
        @ConfigItem
        public Optional<String> secret = Optional.empty();

        /**
         * Client secret which can be used for the 'client_secret_basic' (default) and 'client_secret_post'
         * and 'client_secret_jwt' authentication methods.
         * Note that a 'secret.value' property can be used instead to support the 'client_secret_basic' method
         * but both properties are mutually exclusive.
         */
        @ConfigItem
        public Secret clientSecret = new Secret();

        /**
         * Client JWT authentication methods
         */
        @ConfigItem
        public Jwt jwt = new Jwt();

        public Optional<String> getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = Optional.of(secret);
        }

        public Secret getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(Secret clientSecret) {
            this.clientSecret = clientSecret;
        }

        /**
         * Supports the client authentication methods which involve sending a client secret.
         *
         * @see <a href=
         *      "https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication</a>
         */
        @ConfigGroup
        public static class Secret {

            public static enum Method {
                /**
                 * client_secret_basic (default): client id and secret are submitted with the HTTP Authorization Basic scheme
                 */
                BASIC,

                /**
                 * client_secret_post: client id and secret are submitted as the 'client_id' and 'client_secret' form
                 * parameters.
                 */
                POST
            }

            /**
             * The client secret
             */
            @ConfigItem
            public Optional<String> value = Optional.empty();

            /**
             * Authentication method.
             */
            @ConfigItem
            public Optional<Method> method = Optional.empty();

            public Optional<String> getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = Optional.of(value);
            }

            public Optional<Method> getMethod() {
                return method;
            }

            public void setMethod(Method method) {
                this.method = Optional.of(method);
            }
        }

        /**
         * Supports the client authentication methods which involve sending a signed JWT token.
         * Currently only 'client_secret_jwt' is supported
         *
         * @see <a href=
         *      "https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication</a>
         */
        @ConfigGroup
        public static class Jwt {
            /**
             * client_secret_jwt: JWT which includes client id as one of its claims is signed by the client secret and is
             * submitted as a 'client_assertion' form parameter, while 'client_assertion_type' parameter is set to
             * "urn:ietf:params:oauth:client-assertion-type:jwt-bearer".
             */
            @ConfigItem
            public Optional<String> secret = Optional.empty();

            /**
             * JWT life-span in seconds. It will be added to the time it was issued at to calculate the expiration time.
             */
            @ConfigItem(defaultValue = "10")
            public int lifespan = 10;

            public Optional<String> getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = Optional.of(secret);
            }

            public int getLifespan() {
                return lifespan;
            }

            public void setLifespan(int lifespan) {
                this.lifespan = lifespan;
            }
        }
    }

    @ConfigGroup
    public static class Roles {

        public static Roles fromClaimPath(String path) {
            return fromClaimPathAndSeparator(path, null);
        }

        public static Roles fromClaimPathAndSeparator(String path, String sep) {
            Roles roles = new Roles();
            roles.roleClaimPath = Optional.ofNullable(path);
            roles.roleClaimSeparator = Optional.ofNullable(sep);
            return roles;
        }

        /**
         * Path to the claim containing an array of groups. It starts from the top level JWT JSON object and
         * can contain multiple segments where each segment represents a JSON object name only, example: "realm/groups".
         * Use double quotes with the namespace qualified claim names.
         * This property can be used if a token has no 'groups' claim but has the groups set in a different claim.
         */
        @ConfigItem
        public Optional<String> roleClaimPath = Optional.empty();
        /**
         * Separator for splitting a string which may contain multiple group values.
         * It will only be used if the "role-claim-path" property points to a custom claim whose value is a string.
         * A single space will be used by default because the standard 'scope' claim may contain a space separated sequence.
         */
        @ConfigItem
        public Optional<String> roleClaimSeparator = Optional.empty();

        /**
         * Source of the principal roles.
         */
        @ConfigItem
        public Optional<Source> source = Optional.empty();

        public Optional<String> getRoleClaimPath() {
            return roleClaimPath;
        }

        public void setRoleClaimPath(String roleClaimPath) {
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
         * Relative path for calculating a "redirect_uri" query parameter.
         * It has to start from a forward slash and will be appended to the request URI's host and port.
         * For example, if the current request URI is 'https://localhost:8080/service' then a 'redirect_uri' parameter
         * will be set to 'https://localhost:8080/' if this property is set to '/' and be the same as the request URI
         * if this property has not been configured.
         * Note the original request URI will be restored after the user has authenticated.
         */
        @ConfigItem
        public Optional<String> redirectPath = Optional.empty();

        /**
         * If this property is set to 'true' then the original request URI which was used before
         * the authentication will be restored after the user has been redirected back to the application.
         */
        @ConfigItem(defaultValue = "true")
        public boolean restorePathAfterRedirect = true;

        /**
         * Remove the query parameters such as 'code' and 'state' set by the OIDC server on the redirect URI
         * after the user has authenticated by redirecting a user to the same URI but without the query parameters.
         */
        @ConfigItem(defaultValue = "true")
        public boolean removeRedirectParameters = true;

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
        @ConfigItem(defaultValue = "false")
        public boolean forceRedirectHttpsScheme;

        /**
         * List of scopes
         */
        @ConfigItem
        public Optional<List<String>> scopes = Optional.empty();

        /**
         * Additional properties which will be added as the query parameters to the authentication redirect URI.
         */
        @ConfigItem
        public Map<String, String> extraParams;

        /**
         * Cookie path parameter value which, if set, will be used for the session, state and post logout cookies.
         * It may need to be set when the redirect path has a root different to that of the original request URL.
         */
        @ConfigItem
        public Optional<String> cookiePath = Optional.empty();

        /**
         * Cookie domain parameter value which, if set, will be used for the session, state and post logout cookies.
         */
        @ConfigItem
        public Optional<String> cookieDomain = Optional.empty();

        /**
         * If this property is set to 'true' then an OIDC UserInfo endpoint will be called
         */
        @ConfigItem(defaultValue = "false")
        public boolean userInfoRequired;

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

        public void setScopes(Optional<List<String>> scopes) {
            this.scopes = scopes;
        }

        public Map<String, String> getExtraParams() {
            return extraParams;
        }

        public void setExtraParams(Map<String, String> extraParams) {
            this.extraParams = extraParams;
        }

        public boolean isForceRedirectHttpsScheme() {
            return forceRedirectHttpsScheme;
        }

        public void setForceRedirectHttpsScheme(boolean forceRedirectHttpsScheme) {
            this.forceRedirectHttpsScheme = forceRedirectHttpsScheme;
        }

        public boolean isRestorePathAfterRedirect() {
            return restorePathAfterRedirect;
        }

        public void setRestorePathAfterRedirect(boolean restorePathAfterRedirect) {
            this.restorePathAfterRedirect = restorePathAfterRedirect;
        }

        public Optional<String> getCookiePath() {
            return cookiePath;
        }

        public void setCookiePath(String cookiePath) {
            this.cookiePath = Optional.of(cookiePath);
        }

        public Optional<String> getCookieDomain() {
            return cookieDomain;
        }

        public void setCookieDomain(String cookieDomain) {
            this.cookieDomain = Optional.of(cookieDomain);
        }

        public boolean isUserInfoRequired() {
            return userInfoRequired;
        }

        public void setUserInfoRequired(boolean userInfoRequired) {
            this.userInfoRequired = userInfoRequired;
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
         */
        @ConfigItem
        public Optional<String> issuer = Optional.empty();

        /**
         * Expected audience 'aud' claim value which may be a string or an array of strings.
         */
        @ConfigItem
        public Optional<List<String>> audience = Optional.empty();

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
         * Token auto-refresh interval in seconds during the user re-authentication.
         * If this option is set then the valid ID token will be refreshed if it will expire in less than a number of minutes
         * set by this option. The user will still be authenticated if the ID token can no longer be refreshed but is still
         * valid.
         * This option will be ignored if the 'refresh-expired' property is not enabled.
         */
        @ConfigItem
        public Optional<Duration> autoRefreshInterval = Optional.empty();

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
    }

    @ConfigGroup
    public static class Proxy {

        /**
         * The host (name or IP address) of the Proxy.<br/>
         * Note: If OIDC adapter needs to use a Proxy to talk with OIDC server (Provider),
         * then at least the "host" config item must be configured to enable the usage of a Proxy.
         */
        @ConfigItem
        public Optional<String> host = Optional.empty();

        /**
         * The port number of the Proxy. Default value is 80.
         */
        @ConfigItem(defaultValue = "80")
        public int port = 80;

        /**
         * The username, if Proxy needs authentication.
         */
        @ConfigItem
        public Optional<String> username = Optional.empty();

        /**
         * The password, if Proxy needs authentication.
         */
        @ConfigItem
        public Optional<String> password = Optional.empty();

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

    public ApplicationType getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(ApplicationType type) {
        this.applicationType = type;
    }
}
