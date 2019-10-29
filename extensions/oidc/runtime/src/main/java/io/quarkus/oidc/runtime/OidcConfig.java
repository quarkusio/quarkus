package io.quarkus.oidc.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class OidcConfig {

    /**
     * If the OIDC extension is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The base URL of the OpenID Connect (OIDC) server, for example, 'https://host:port/auth'.
     * All the other OIDC server page and service URLs are derived from this URL.
     * Note if you work with Keycloak OIDC server, make sure the base URL is in the following format:
     * 'https://host:port/auth/realms/{realm}' where '{realm}' has to be replaced by the name of the Keycloak realm.
     */
    @ConfigItem
    String authServerUrl;

    /**
     * Relative path of the RFC7662 introspection service.
     */
    @ConfigItem
    Optional<String> introspectionPath;

    /**
     * Relative path of the OIDC service returning a JWK set.
     */
    @ConfigItem
    Optional<String> jwksPath;

    /**
     * Public key for the local JWT token verification.
     */
    @ConfigItem
    Optional<String> publicKey;

    /**
     * The client-id of the application. Each application has a client-id that is used to identify the application
     */
    @ConfigItem
    Optional<String> clientId;

    /**
     * Configuration to find and parse a custom claim containing the roles information.
     */
    @ConfigItem
    Roles roles;

    /**
     * Credentials which the OIDC adapter will use to authenticate to the OIDC server.
     */
    @ConfigItem
    Credentials credentials;

    /**
     * Different options to configure authorization requests
     */
    Authentication authentication;

    /**
     * The application type, which can be one of the following values from enum {@link ApplicationType}..
     */
    @ConfigItem(defaultValue = "service")
    ApplicationType applicationType;

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public Optional<String> getClientId() {
        return clientId;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public Roles getRoles() {
        return roles;
    }

    public ApplicationType getApplicationType() {
        return applicationType;
    }

    @ConfigGroup
    public static class Credentials {

        /**
         * The client secret
         */
        @ConfigItem
        Optional<String> secret;

        public Optional<String> getSecret() {
            return secret;
        }
    }

    @ConfigGroup
    public static class Roles {

        /**
         * Path to the claim containing an array of groups. It starts from the top level JWT JSON object and
         * can contain multiple segments where each segment represents a JSON object name only, example: "realm/groups".
         * This property can be used if a token has no 'groups' claim but has the groups set in a different claim.
         */
        @ConfigItem
        Optional<String> roleClaimPath;

        /**
         * Separator for splitting a string which may contain multiple group values.
         * It will only be used if the "role-claim-path" property points to a custom claim whose value is a string.
         * A single space will be used by default because the standard 'scope' claim may contain a space separated sequence.
         */
        @ConfigItem
        Optional<String> roleClaimSeparator;

        public Optional<String> getRoleClaimPath() {
            return roleClaimPath;
        }

        public Optional<String> getRoleClaimSeparator() {
            return roleClaimSeparator;
        }

        public static Roles fromClaimPath(String path) {
            return fromClaimPathAndSeparator(path, null);
        }

        public static Roles fromClaimPathAndSeparator(String path, String sep) {
            Roles roles = new Roles();
            roles.roleClaimPath = Optional.ofNullable(path);
            roles.roleClaimSeparator = Optional.ofNullable(sep);
            return roles;
        }
    }

    @ConfigGroup
    public static class Authentication {

        /**
         * Defines a fixed list of scopes which should be added to authorization requests when authenticating users using the
         * Authorization Code Grant Type.
         *
         */
        @ConfigItem
        public List<String> scopes;
    }

    public enum ApplicationType {
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
