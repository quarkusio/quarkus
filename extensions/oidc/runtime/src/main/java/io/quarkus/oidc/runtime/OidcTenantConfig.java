package io.quarkus.oidc.runtime;

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
    Optional<String> authServerUrl = Optional.empty();
    /**
     * Relative path of the RFC7662 introspection service.
     */
    @ConfigItem
    Optional<String> introspectionPath = Optional.empty();
    /**
     * Relative path of the OIDC service returning a JWK set.
     */
    @ConfigItem
    Optional<String> jwksPath = Optional.empty();
    /**
     * Public key for the local JWT token verification.
     */
    @ConfigItem
    Optional<String> publicKey = Optional.empty();
    /**
     * The client-id of the application. Each application has a client-id that is used to identify the application
     */
    @ConfigItem
    Optional<String> clientId = Optional.empty();
    /**
     * Configuration to find and parse a custom claim containing the roles information.
     */
    @ConfigItem
    Roles roles = new Roles();
    /**
     * Configuration how to validate the token claims.
     */
    @ConfigItem
    Token token = new Token();
    /**
     * Credentials which the OIDC adapter will use to authenticate to the OIDC server.
     */
    @ConfigItem
    Credentials credentials = new Credentials();
    /**
     * Different options to configure authorization requests
     */
    Authentication authentication = new Authentication();

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

    @ConfigGroup
    public static class Credentials {

        /**
         * The client secret
         */
        @ConfigItem
        Optional<String> secret = Optional.empty();

        public Optional<String> getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = Optional.of(secret);
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
        Optional<String> roleClaimPath = Optional.empty();
        /**
         * Separator for splitting a string which may contain multiple group values.
         * It will only be used if the "role-claim-path" property points to a custom claim whose value is a string.
         * A single space will be used by default because the standard 'scope' claim may contain a space separated sequence.
         */
        @ConfigItem
        Optional<String> roleClaimSeparator = Optional.empty();

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
         * Expiration grace period in seconds. A token expiration time will be reduced by
         * the value of this property before being compared to the current time.
         */
        @ConfigItem
        public Optional<Integer> expirationGrace = Optional.empty();

        /**
         * Name of the claim which contains a principal name. By default, the 'upn', 'preferred_username' and `sub` claims are
         * checked.
         */
        @ConfigItem
        public Optional<String> principalClaim = Optional.empty();

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

        public Optional<Integer> getExpirationGrace() {
            return expirationGrace;
        }

        public void setExpirationGrace(int expirationGrace) {
            this.expirationGrace = Optional.of(expirationGrace);
        }

        public Optional<String> getPrincipalClaim() {
            return principalClaim;
        }

        public void setPrincipalClaim(String principalClaim) {
            this.principalClaim = Optional.of(principalClaim);
        }
    }
}
