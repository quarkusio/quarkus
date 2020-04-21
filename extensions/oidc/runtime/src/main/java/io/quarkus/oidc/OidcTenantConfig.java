package io.quarkus.oidc;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public ApplicationType applicationType;

    /**
     * The maximum amount of time the adapter will try connecting to the currently unavailable OIDC server for.
     * For example, setting it to '20S' will let the adapter keep requesting the connection for up to 20 seconds.
     */
    @ConfigItem
    public Optional<Duration> connectionDelay = Optional.empty();

    /**
     * The base URL of the OpenID Connect (OIDC) server, for example, 'https://host:port/auth'.
     * All the other OIDC server page and service URLs are derived from this URL.
     * Note if you work with Keycloak OIDC server, make sure the base URL is in the following format:
     * 'https://host:port/auth/realms/{realm}' where '{realm}' has to be replaced by the name of the Keycloak realm.
     */
    @ConfigItem
    public Optional<String> authServerUrl = Optional.empty();

    /**
     * Relative path of the RFC7662 introspection service.
     */

    @ConfigItem
    public Optional<String> introspectionPath = Optional.empty();

    /**
     * Relative path of the OIDC service returning a JWK set.
     */
    @ConfigItem
    public Optional<String> jwksPath = Optional.empty();

    /**
     * Relative path of the OIDC end_session_endpoint.
     */
    @ConfigItem
    public Optional<String> endSessionPath = Optional.empty();
    /**
     * Public key for the local JWT token verification.
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
        @ConfigItem(defaultValue = "REQUIRED")
        public Verification verification;

        public Verification getVerification() {
            return verification;
        }

        public void setVerification(Verification verification) {
            this.verification = verification;
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
        public boolean restorePathAfterRedirect;

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
         * Cookie path parameter value which, if set, will be used for the session and state cookies.
         * It may need to be set when the redirect path has a root different to that of the original request URL.
         */
        @ConfigItem
        public Optional<String> cookiePath = Optional.empty();

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
         * Life span grace period in seconds.
         * When checking token expiry, current time is allowed to be later than token expiration time by at most the configured
         * number of seconds.
         * When checking token issuance, current time is allowed to be sooner than token issue time by at most the configured
         * number of seconds.
         */
        @ConfigItem
        public Optional<Integer> lifespanGrace = Optional.empty();

        /**
         * Name of the claim which contains a principal name. By default, the 'upn', 'preferred_username' and `sub` claims are
         * checked.
         */
        @ConfigItem
        public Optional<String> principalClaim = Optional.empty();

        /**
         * Refresh expired ID tokens.
         * If this property is enabled then a refresh token request is performed and, if successful, the local session is
         * updated with the new set of tokens.
         * Otherwise, the local session is invalidated as an indication that the session at the OpenID Provider no longer
         * exists.
         * This option is only valid when the application is of type {@link ApplicationType#WEB_APP}}.
         */
        @ConfigItem
        public boolean refreshExpired;

        public Optional<String> getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = Optional.of(issuer);
        }

        public Optional<List<String>> getAudience() {
            return audience;
        }

        public void setAudience(List<String> audience) {
            this.audience = Optional.of(audience);
        }

        public Optional<Integer> getLifespanGrace() {
            return lifespanGrace;
        }

        public void setLifespanGrace(int lifespanGrace) {
            this.lifespanGrace = Optional.of(lifespanGrace);
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
         * A {@code WEB_APP} is a client that server pages, usually a frontend application. For this type of client the
         * Authorization Code Flow is
         * defined as the preferred method for authenticating users.
         */
        WEB_APP,

        /**
         * A {@code SERVICE} is a client that has a set of protected HTTP resources, usually a backend application following the
         * RESTful Architectural Design. For this type of client, the Bearer Authorization method is defined as the preferred
         * method for authenticating and authorizing users.
         */
        SERVICE
    }
}
