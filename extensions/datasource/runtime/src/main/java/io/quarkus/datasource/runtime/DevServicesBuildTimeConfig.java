package io.quarkus.datasource.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DevServicesBuildTimeConfig {

    /**
     * Whether this Dev Service should start with the application in dev mode or tests.
     *
     * Dev Services are enabled by default
     * unless connection configuration (e.g. the JDBC URL or reactive client URL) is set explicitly.
     *
     * @asciidoclet
     */
    Optional<Boolean> enabled();

    /**
     * The container image name for container-based Dev Service providers.
     *
     * This has no effect if the provider is not a container-based database, such as H2 or Derby.
     *
     * Defaults depend on the configured `datasource`:
     *
     * * DB2: `{db2-image}`
     * * MariaDB: `{mariadb-image}`
     * * Microsoft SQL Server: `{mssql-image}` +
     * On ARM (aarch64),`{mssql-image-aarch64}` is used instead, because the standard MS SQL Server image does not run on ARM.
     * * MySQL: `{mysql-image}`
     * * Oracle Express Edition: `{oracle-image}`
     * * PostgreSQL: `{postgres-image}`
     *
     * For certain databases, the default image may be automatically adjusted based on required database features
     * (e.g., spatial or vector support). Explicitly setting this property overrides the automatic image selection.
     *
     * @asciidoclet
     */
    @ConfigDocDefault("Depends on `db-kind`, see description.")
    Optional<@WithConverter(TrimmedStringConverter.class) String> imageName();

    /**
     * Environment variables that are passed to the container.
     */
    @ConfigDocMapKey("environment-variable-name")
    Map<String, String> containerEnv();

    /**
     * Generic properties that are passed for additional container configuration.
     * <p>
     * Properties defined here are database-specific
     * and are interpreted specifically in each database dev service implementation.
     */
    @ConfigDocMapKey("property-key")
    Map<String, String> containerProperties();

    /**
     * Generic properties that are added to the database connection URL.
     */
    @ConfigDocMapKey("property-key")
    Map<String, String> properties();

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    OptionalInt port();

    /**
     * The container start command to use for container-based Dev Service providers.
     * <p>
     * This has no effect if the provider is not a container-based database, such as H2 or Derby.
     */
    Optional<@WithConverter(TrimmedStringConverter.class) String> command();

    /**
     * The database name to use if this Dev Service supports overriding it.
     */
    Optional<@WithConverter(TrimmedStringConverter.class) String> dbName();

    /**
     * The username to use if this Dev Service supports overriding it.
     */
    Optional<String> username();

    /**
     * The password to use if this Dev Service supports overriding it.
     */
    Optional<String> password();

    /**
     * The paths to SQL scripts to be loaded from the classpath and applied to the Dev Service database.
     * <p>
     * This has no effect if the provider is not a container-based database, such as H2 or Derby.
     */
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> initScriptPath();

    /**
     * The paths to SQL scripts to be loaded from the classpath and applied to the Dev Service database using the SYS privileged
     * user.
     * Not all databases provide a privileged user. In these cases, the property is ignored.
     * This has no effect if the provider is not a container-based database, such as H2 or Derby.
     */
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> initPrivilegedScriptPath();

    /**
     * The volumes to be mapped to the container.
     * <p>
     * The map key corresponds to the host location; the map value is the container location.
     * If the host location starts with "classpath:",
     * the mapping loads the resource from the classpath with read-only permission.
     * <p>
     * When using a file system location, the volume will be generated with read-write permission,
     * potentially leading to data loss or modification in your file system.
     * <p>
     * This has no effect if the provider is not a container-based database, such as H2 or Derby.
     */
    @ConfigDocMapKey("host-path")
    Map<String, String> volumes();

    /**
     * The name of the datasource whose Dev Services container this datasource should reuse, instead of
     * starting a separate container.
     * <p>
     * When set, this datasource does not start its own Dev Services container. Instead, it waits for the
     * named datasource's Dev Services container to start, then reuses its JDBC URL, reactive URL, username
     * and password. The other `devservices.*` properties of this datasource (image name, init scripts,
     * volumes, ...) are then ignored, since no container is started for it.
     * <p>
     * The referenced datasource must set `devservices.shares-container` to `true`, and must not itself set
     * `use-from`; only one level of sharing is supported.
     * <p>
     * Use `<default>` to refer to the default datasource.
     * <p>
     * This only affects Dev Services, within a single application, in dev mode and in tests. It does not
     * make Dev Services containers shared across different applications.
     *
     * @asciidoclet
     */
    Optional<String> useFrom();

    /**
     * Whether this datasource accepts other datasources reusing its Dev Services container, by referencing
     * it through their own `devservices.use-from` property.
     * <p>
     * Has no effect by itself; it only allows other datasources to opt in to reusing this datasource's
     * container. If no other datasource references this one through `devservices.use-from`, this property
     * changes nothing.
     *
     * @asciidoclet
     */
    @WithDefault("false")
    boolean sharesContainer();

    /**
     * Whether to keep Dev Service containers running *after a dev mode session or test suite execution*
     * to reuse them in the next dev mode session or test suite execution.
     *
     * Within a dev mode session or test suite execution,
     * Quarkus will always reuse Dev Services as long as their configuration
     * (username, password, environment, port bindings, ...) did not change.
     * This feature is specifically about keeping containers running
     * **when Quarkus is not running** to reuse them across runs.
     *
     * WARNING: This feature needs to be enabled explicitly in `testcontainers.properties`,
     * may require changes to how you configure data initialization in dev mode and tests,
     * and may leave containers running indefinitely, forcing you to stop and remove them manually.
     * See xref:databases-dev-services.adoc#reuse[this section of the documentation] for more information.
     *
     * This configuration property is set to `true` by default,
     * so it is mostly useful to *disable* reuse,
     * if you enabled it in `testcontainers.properties`
     * but only want to use it for some of your Quarkus applications or datasources.
     *
     * @asciidoclet
     */
    @WithDefault("true")
    boolean reuse();

    /**
     * Whether the logs should be consumed by the JBoss logger.
     * <p>
     * This has no effect if the provider is not a container-based database, such as H2 or Derby.
     */
    @WithDefault("false")
    boolean showLogs();

}
