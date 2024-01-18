package io.quarkus.oidc.deployment.devservices.keycloak;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.oidc.deployment.DevUiConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DevServicesConfig {

    /**
     * Flag to enable (default) or disable Dev Services.
     *
     * When enabled, Dev Services for Keycloak automatically configures and starts Keycloak in Dev or Test mode, and when Docker
     * is running.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled = true;

    /**
     * The container image name for Dev Services providers.
     *
     * Defaults to a Quarkus-based Keycloak image. For a WildFly-based distribution, use an image like
     * `quay.io/keycloak/keycloak:19.0.3-legacy`.
     *
     * Keycloak Quarkus and WildFly images are initialized differently. Dev Services for Keycloak will assume it is a Keycloak
     * Quarkus image unless the image version
     * ends with `-legacy`.
     * Override with `quarkus.keycloak.devservices.keycloak-x-image`.
     */
    @ConfigItem(defaultValue = "quay.io/keycloak/keycloak:23.0.4")
    public String imageName;

    /**
     * Indicates if a Keycloak-X image is used.
     *
     * By default, the image is identified by `keycloak-x` in the image name.
     * For custom images, override with `quarkus.keycloak.devservices.keycloak-x-image`.
     * You do not need to set this property if the default check works.
     */
    @ConfigItem
    public Optional<Boolean> keycloakXImage;

    /**
     * Determines if the Keycloak container is shared.
     *
     * When shared, Quarkus uses label-based service discovery to find and reuse a running Keycloak container, so a second one
     * is not started.
     * Otherwise, if a matching container is not is found, a new container is started.
     *
     * The service discovery uses the {@code quarkus-dev-service-label} label, whose value is set by the {@code service-name}
     * property.
     *
     * Container sharing is available only in dev mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean shared;

    /**
     * The value of the {@code quarkus-dev-service-keycloak} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Keycloak looks for a container with the
     * {@code quarkus-dev-service-keycloak} label
     * set to the configured value. If found, it uses this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-keycloak} label set to the specified value.
     * <p>
     * Container sharing is only used in dev mode.
     */
    /**
     * The value of the `quarkus-dev-service-keycloak` label for identifying the Keycloak container.
     *
     * Used in shared mode to locate an existing container with this label. If not found, a new container is initialized with
     * this label.
     *
     * Applicable only in dev mode.
     */
    @ConfigItem(defaultValue = "quarkus")
    public String serviceName;

    /**
     * A comma-separated list of class or file system paths to Keycloak realm files.
     * This list is used to initialize Keycloak.
     * The first value in this list is used to initialize default tenant connection properties.
     */
    @ConfigItem
    public Optional<List<String>> realmPath;

    /**
     * Aliases to additional class or file system resources that are used to initialize Keycloak.
     * Each map entry represents a mapping between an alias and a class or file system resource path.
     */
    @ConfigItem
    public Map<String, String> resourceAliases;
    /**
     * Additional class or file system resources that are used to initialize Keycloak.
     * Each map entry represents a mapping between a class or file system resource path alias and the Keycloak container
     * location.
     */
    @ConfigItem
    public Map<String, String> resourceMappings;

    /**
     * The `JAVA_OPTS` passed to the keycloak JVM
     */
    @ConfigItem
    public Optional<String> javaOpts;

    /**
     * Show Keycloak log messages with a "Keycloak:" prefix.
     */
    @ConfigItem(defaultValue = "false")
    public boolean showLogs;

    /**
     * Keycloak start command.
     * Use this property to experiment with Keycloak start options, see {@link https://www.keycloak.org/server/all-config}.
     * Note, it is ignored when loading legacy Keycloak WildFly images.
     */
    @ConfigItem
    public Optional<String> startCommand;

    /**
     * The name of the Keycloak realm.
     *
     * This property is used to create the realm if the realm file pointed to by the `realm-path` property does not exist.
     * The default value is `quarkus` in this case.
     * It is recommended to always set this property so that Dev Services for Keycloak can identify the realm name without
     * parsing the realm file.
     */
    @ConfigItem
    public Optional<String> realmName;

    /**
     * Specifies whether to create the Keycloak realm when no realm file is found at the `realm-path`.
     *
     * Set to `false` if the realm is to be created using either the Keycloak Administration Console or
     * the Keycloak Admin API provided by {@linkplain io.quarkus.test.common.QuarkusTestResourceLifecycleManager}.
     */
    @ConfigItem(defaultValue = "true")
    public boolean createRealm;

    /**
     * A map of Keycloak usernames to passwords.
     *
     * If empty, default users `alice` and `bob` are created with their names as passwords.
     * This map is used for user creation when no realm file is found at the `realm-path`.
     */
    @ConfigItem
    public Map<String, String> users;

    /**
     * A map of roles for Keycloak users.
     *
     * If empty, default roles are assigned: `alice` receives `admin` and `user` roles, while other users receive
     * `user` role.
     * This map is used for role creation when no realm file is found at the `realm-path`.
     */
    @ConfigItem
    public Map<String, List<String>> roles;

    /**
     * Specifies the grant type.
     *
     * @deprecated This field is deprecated. Use {@link DevUiConfig#grant} instead.
     */
    @Deprecated
    public Grant grant = new Grant();

    @ConfigGroup
    public static class Grant {
        public static enum Type {
            /**
             * `client_credentials` grant
             */
            CLIENT("client_credentials"),
            /**
             * `password` grant
             */
            PASSWORD("password"),

            /**
             * `authorization_code` grant
             */
            CODE("code"),

            /**
             * `implicit` grant
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
         * Defines the grant type for aquiring tokens for testing OIDC `service` applications.
         */
        @ConfigItem(defaultValue = "code")
        public Type type = Type.CODE;
    }

    /**
     * The specific port for the dev service to listen on.
     * <p>
     * If not specified, a random port is selected.
     */
    @ConfigItem
    public OptionalInt port;

    /**
     * Environment variables to be passed to the container.
     */
    @ConfigItem
    public Map<String, String> containerEnv;

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
                && Objects.equals(roles, that.roles)
                && Objects.equals(containerEnv, that.containerEnv);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, imageName, port, realmPath, realmName, users, roles, containerEnv);
    }
}
