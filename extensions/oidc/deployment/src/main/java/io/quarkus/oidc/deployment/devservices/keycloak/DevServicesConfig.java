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
     * If DevServices has been explicitly enabled or disabled.
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
    public String imageName;

    /**
     * Indicates if the Keycloak container managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for Keycloak starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-label} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean shared;

    /**
     * The value of the {@code quarkus-dev-service-keycloak} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Keycloak looks for a container with the
     * {@code quarkus-dev-service-keycloak} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise it
     * starts a new container with the {@code quarkus-dev-service-keycloak} label set to the specified value.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @ConfigItem(defaultValue = "quarkus")
    public String serviceName;

    /**
     * The class or file system path to a Keycloak realm file which will be used to initialize Keycloak.
     */
    @ConfigItem
    public Optional<String> realmPath;

    /**
     * The JAVA_OPTS passed to the keycloak JVM
     */
    @ConfigItem
    public Optional<String> javaOpts;

    /**
     * The Keycloak realm name.
     * This property will be used to create the realm if the realm file pointed to by the 'realm-path' property does not exist,
     * default value is 'quarkus' in this case.
     * If the realm file pointed to by the 'realm-path' property exists then it is still recommended to set this property
     * for Dev Services for Keycloak to avoid parsing the realm file in order to determine the realm name.
     *
     */
    @ConfigItem
    public Optional<String> realmName;

    /**
     * Indicates if the Keycloak realm has to be created when the realm file pointed to by the 'realm-path' property does not
     * exist.
     *
     * Disable it if you'd like to create a realm using Keycloak Administration Console
     * or Keycloak Admin API from {@linkplain io.quarkus.test.common.QuarkusTestResourceLifecycleManager}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean createRealm;

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
             * 'authorization_code' grant
             */
            CODE("code"),

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
        @ConfigItem(defaultValue = "code")
        public Type type = Type.CODE;
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
                && Objects.equals(javaOpts, that.javaOpts)
                && Objects.equals(roles, that.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, imageName, port, realmPath, realmName, users, roles);
    }
}
