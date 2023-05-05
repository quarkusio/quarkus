package io.quarkus.datasource.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface DevServicesBuildTimeConfig {

    /**
     * If DevServices has been explicitly enabled or disabled. DevServices is generally enabled
     * by default, unless there is an existing configuration present.
     *
     * When DevServices is enabled Quarkus will attempt to automatically configure and start
     * a database when running in Dev or Test mode.
     */
    Optional<Boolean> enabled();

    /**
     * The container image name to use, for container based DevServices providers.
     *
     * If the provider is not container based (e.g. a H2 Database) then this has no effect.
     */
    Optional<String> imageName();

    /**
     * Environment variables that are passed to the container.
     */
    Map<String, String> containerEnv();

    /**
     * Generic properties that are passed for additional container configuration.
     * <p>
     * Properties defined here are database specific and are interpreted specifically in each database dev service
     * implementation.
     */
    Map<String, String> containerProperties();

    /**
     * Generic properties that are added to the database connection URL.
     */
    Map<String, String> properties();

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    OptionalInt port();

    /**
     * The container start command to use, for container based DevServices providers.
     *
     * If the provider is not container based (e.g. a H2 Database) then this has no effect.
     */
    Optional<String> command();

    /**
     * The name of the database to use if this Dev Service supports overriding it.
     */
    Optional<String> dbName();

    /**
     * The username to use if this Dev Service supports overriding it.
     */
    Optional<String> username();

    /**
     * The password to use if this Dev Service supports overriding it.
     */
    Optional<String> password();

    /**
     * Path to a SQL script that will be loaded from the classpath and applied to the Dev Service database
     *
     * If the provider is not container based (e.g. an H2 or Derby Database) then this has no effect.
     */
    Optional<String> initScriptPath();

    /**
     * The volumes to be mapped to the container. The map key corresponds to the host location and the map value is the
     * container location. If the host location starts with "classpath:", then the mapping will load the resource from the
     * classpath with read-only permission.
     *
     * When using a file system location, the volume will be created with read-write permission, so the data in your file
     * system might be wiped out or altered.
     *
     * If the provider is not container based (e.g. an H2 or Derby Database) then this has no effect.
     */
    Map<String, String> volumes();
}
