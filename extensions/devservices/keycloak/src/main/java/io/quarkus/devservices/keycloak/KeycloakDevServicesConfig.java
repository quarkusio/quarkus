package io.quarkus.devservices.keycloak;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build time configuration for the Keycloak Dev Service.
 */
@ConfigMapping(prefix = "quarkus.keycloak.devservices")
@ConfigRoot
public interface KeycloakDevServicesConfig {

    /**
     * Flag to enable (default) or disable Dev Services.
     *
     * When enabled, Dev Services for Keycloak automatically configures and starts Keycloak in Dev or Test mode, and when Docker
     * is running.
     */
    @WithDefault("true")
    boolean enabled();

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
    @WithDefault("quay.io/keycloak/keycloak:26.0.7")
    String imageName();

    /**
     * Indicates if a Keycloak-X image is used.
     *
     * By default, the image is identified by `keycloak-x` in the image name.
     * For custom images, override with `quarkus.keycloak.devservices.keycloak-x-image`.
     * You do not need to set this property if the default check works.
     */
    Optional<Boolean> keycloakXImage();

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
    @WithDefault("true")
    boolean shared();

    /**
     * The value of the `quarkus-dev-service-keycloak` label for identifying the Keycloak container.
     *
     * Used in shared mode to locate an existing container with this label. If not found, a new container is initialized with
     * this label.
     *
     * Applicable only in dev mode.
     */
    @WithDefault("quarkus")
    String serviceName();

    /**
     * A comma-separated list of class or file system paths to Keycloak realm files.
     * This list is used to initialize Keycloak.
     * The first value in this list is used to initialize default tenant connection properties.
     * <p>
     * To learn more about Keycloak realm files, consult the <a href="https://www.keycloak.org/server/importExport">Importing
     * and Exporting Keycloak Realms documentation</a>.
     */
    Optional<List<String>> realmPath();

    /**
     * Aliases to additional class or file system resources that are used to initialize Keycloak.
     * Each map entry represents a mapping between an alias and a class or file system resource path.
     */
    @ConfigDocMapKey("alias-name")
    Map<String, String> resourceAliases();

    /**
     * Additional class or file system resources that are used to initialize Keycloak.
     * Each map entry represents a mapping between a class or file system resource path alias and the Keycloak container
     * location.
     */
    @ConfigDocMapKey("resource-name")
    Map<String, String> resourceMappings();

    /**
     * The `JAVA_OPTS` passed to the keycloak JVM
     */
    Optional<String> javaOpts();

    /**
     * Show Keycloak log messages with a "Keycloak:" prefix.
     */
    @WithDefault("false")
    boolean showLogs();

    /**
     * Keycloak start command.
     * Use this property to experiment with Keycloak start options, see {@link https://www.keycloak.org/server/all-config}.
     * Note, it is ignored when loading legacy Keycloak WildFly images.
     */
    Optional<String> startCommand();

    /**
     * The name of the Keycloak realm.
     *
     * This property is used to create the realm if the realm file pointed to by the `realm-path` property does not exist.
     * The default value is `quarkus` in this case.
     * It is recommended to always set this property so that Dev Services for Keycloak can identify the realm name without
     * parsing the realm file.
     */
    Optional<String> realmName();

    /**
     * Specifies whether to create the Keycloak realm when no realm file is found at the `realm-path`.
     *
     * Set to `false` if the realm is to be created using either the Keycloak Administration Console or
     * the Keycloak Admin API provided by {@linkplain io.quarkus.test.common.QuarkusTestResourceLifecycleManager}.
     */
    @WithDefault("true")
    boolean createRealm();

    /**
     * Specifies whether to create the default client id `quarkus-app` with a secret `secret` and register them
     * if the {@link #createRealm} property is set to true.
     * For OIDC extension configuration properties `quarkus.oidc.client.id` and `quarkus.oidc.credentials.secret` will
     * be configured.
     * For OIDC Client extension configuration properties `quarkus.oidc-client.client.id`
     * and `quarkus.oidc-client.credentials.secret` will be configured.
     *
     * Set to `false` if clients have to be created using either the Keycloak Administration Console or
     * the Keycloak Admin API provided by {@linkplain io.quarkus.test.common.QuarkusTestResourceLifecycleManager}
     * or registered dynamically.
     */
    @WithDefault("true")
    boolean createClient();

    /**
     * Specifies whether to start the container even if the default OIDC tenant is disabled.
     *
     * Setting this property to true may be necessary in a multi-tenant OIDC setup, especially when OIDC tenants are created
     * dynamically.
     */
    @WithDefault("false")
    boolean startWithDisabledTenant();

    /**
     * A map of Keycloak usernames to passwords.
     *
     * If empty, default users `alice` and `bob` are created with their names as passwords.
     * This map is used for user creation when no realm file is found at the `realm-path`.
     */
    Map<String, String> users();

    /**
     * A map of roles for Keycloak users.
     *
     * If empty, default roles are assigned: `alice` receives `admin` and `user` roles, while other users receive
     * `user` role.
     * This map is used for role creation when no realm file is found at the `realm-path`.
     */
    @ConfigDocMapKey("role-name")
    Map<String, List<String>> roles();

    /**
     * The specific port for the dev service to listen on.
     * <p>
     * If not specified, a random port is selected.
     */
    OptionalInt port();

    /**
     * Environment variables to be passed to the container.
     */
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> containerEnv();

    /**
     * Memory limit for Keycloak container
     * </p>
     * If not specified, 750MiB is the default memory limit.
     */
    @WithDefault("750M")
    MemorySize containerMemoryLimit();

    /**
     * The WebClient timeout.
     * Use this property to configure how long an HTTP client used by OIDC dev service admin client will wait
     * for a response from OpenId Connect Provider when acquiring admin token and creating realm.
     */
    @WithDefault("4S")
    Duration webClientTimeout();

}
