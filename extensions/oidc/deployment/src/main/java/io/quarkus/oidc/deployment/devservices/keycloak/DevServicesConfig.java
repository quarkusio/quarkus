package io.quarkus.oidc.deployment.devservices.keycloak;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DevServicesConfig {

    /**
     * If DevServices has been explicitly enabled or disabled..
     * <p>
     * When DevServices is enabled Quarkus will attempt to automatically configure and start
     * Keycloak when running in Dev or Test mode and when Docker is running.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled = true;

    /**
     * The container image name to use, for container based DevServices providers.
     */
    @ConfigItem(defaultValue = "quay.io/keycloak/keycloak:14.0.0")
    public Optional<String> imageName;

    /**
     * The class or file system path to a Keycloak realm file which will be used to initialize Keycloak.
     */
    @ConfigItem
    public Optional<String> realmPath;

    /**
     * The Keycloak realm.
     * This property will be used to create the realm if the realm file pointed to by the 'realm-path' property does not exist.
     * Setting this property is recommended even if realm file exists
     * for `quarkus.oidc.auth-server-url` property be correctly calculated.
     */
    @ConfigItem(defaultValue = "quarkus")
    public String realmName;

    /**
     * The Keycloak users map containing the user name and password pairs.
     * If this map is empty then two users, 'alice' and 'bob' with the passwords matching their names will be created.
     * This property will be used to create the Keycloak users if the realm file pointed to by the 'realm-path' property does
     * not exist.
     */
    @ConfigItem
    public Map<String, String> users;

    /**
     * The Keycloak user roles.
     * If this map is empty then a user named 'alice' will get 'admin' and 'user' roles and all other users will get a 'user'
     * role.
     * This property will be used to create the Keycloak roles if the realm file pointed to by the 'realm-path' property does
     * not exist.
     */
    @ConfigItem
    public Map<String, String> roles;

    public Grant grant = new Grant();

    @ConfigGroup
    public static class Grant {
        public static enum Type {
            /**
             * 'client_credentials' grant
             */
            CLIENT("client_credentials"),
            /**
             * 'password' grant
             */
            PASSWORD("password"),

            /**
             * 'implicit' grant
             */
            IMPLICIT("implicit");

            private String grantType;

            private Type(String grantType) {
                this.grantType = grantType;
            }

            public String getGrantType() {
                return grantType;
            }
        }

        /**
         * Grant type which will be used to acquire a token to test the OIDC 'service' applications
         */
        @ConfigItem(defaultValue = "implicit")
        public Type type = Type.IMPLICIT;
    }

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    @ConfigItem
    public OptionalInt port;

    /**
     * The WebClient timeout.
     * Use this property to configure how long an HTTP client will wait for a response when requesting
     * tokens from Keycloak and sending them to the service endpoint.
     */
    @ConfigItem(defaultValue = "4S")
    public Duration webClienTimeout;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DevServicesConfig that = (DevServicesConfig) o;
        // grant.type is not checked since it only affects which grant is used by the Dev UI provider.html
        // and as such the changes to this property should not cause restarting a container
        return enabled == that.enabled
                && Objects.equals(imageName, that.imageName)
                && Objects.equals(port, that.port)
                && Objects.equals(realmPath, that.realmPath)
                && Objects.equals(realmName, that.realmName)
                && Objects.equals(users, that.users)
                && Objects.equals(roles, that.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, imageName, port, realmPath, realmName, users, roles);
    }
}
