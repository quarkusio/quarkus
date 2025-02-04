package io.quarkus.flyway.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface FlywayDataSourceRuntimeConfig {

    /**
     * Flag to activate/deactivate Flyway for a specific datasource at runtime.
     */
    @ConfigDocDefault("'true' if the datasource is active; 'false' otherwise")
    Optional<Boolean> active();

    /**
     * The maximum number of retries when attempting to connect to the database.
     * <p>
     * After each failed attempt, Flyway will wait up to the configured `connect-retries-interval` duration before
     * attempting to connect again, up to the maximum number of times specified by connectRetries.
     */
    OptionalInt connectRetries();

    /**
     * The maximum time between retries when attempting to connect to the database.
     * <p>
     * This will cap the interval between connect retries to the value provided.
     */
    @ConfigDocDefault("120 seconds")
    Optional<Duration> connectRetriesInterval();

    /**
     * Sets the default schema managed by Flyway. This schema name is case-sensitive. If not specified, but <i>schemas</i>
     * is, Flyway uses the first schema in that list. If that is also not specified, Flyway uses the default schema for the
     * database connection.
     * <p>
     * Consequences:
     * </p>
     * <ul>
     * <li>This schema will be the one containing the schema history table.</li>
     * <li>This schema will be the default for the database connection (provided the database supports this concept).</li>
     * </ul>
     */
    Optional<String> defaultSchema();

    /**
     * The JDBC URL that Flyway uses to connect to the database.
     * Falls back to the datasource URL if not specified.
     */
    Optional<String> jdbcUrl();

    /**
     * The username that Flyway uses to connect to the database.
     * If no specific JDBC URL is configured, falls back to the datasource username if not specified.
     */
    Optional<String> username();

    /**
     * The password that Flyway uses to connect to the database.
     * If no specific JDBC URL is configured, falls back to the datasource password if not specified.
     */
    Optional<String> password();

    /**
     * Comma-separated case-sensitive list of schemas managed by Flyway.
     * The first schema in the list will be automatically set as the default one during the migration.
     * It will also be the one containing the schema history table.
     */
    Optional<List<String>> schemas();

    /**
     * The name of Flyway's schema history table.
     * By default (single-schema mode), the schema history table is placed in the default schema for the connection provided by
     * the datasource.
     * When the flyway.schemas property is set (multi-schema mode), the schema history table is placed in the first schema of
     * the list.
     */
    Optional<String> table();

    /**
     * The file name prefix for versioned SQL migrations.
     *
     * Versioned SQL migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix , which using
     * the defaults translates to V1.1__My_description.sql
     */
    Optional<String> sqlMigrationPrefix();

    /**
     * The file name prefix for repeatable SQL migrations.
     *
     * Repeatable SQL migrations have the following file name structure: prefixSeparatorDESCRIPTIONsuffix , which using the
     * defaults translates to R__My_description.sql
     */
    Optional<String> repeatableSqlMigrationPrefix();

    /**
     * true to execute Flyway clean command automatically when the application starts, false otherwise.
     *
     */
    @WithDefault("false")
    boolean cleanAtStart();

    /**
     * true to prevent Flyway clean operations, false otherwise.
     */
    @WithDefault("false")
    boolean cleanDisabled();

    /**
     * true to execute Flyway automatically when the application starts, false otherwise.
     *
     */
    @WithDefault("false")
    boolean migrateAtStart();

    /**
     * true to execute a Flyway repair command when the application starts, false otherwise.
     *
     */
    @WithDefault("false")
    boolean repairAtStart();

    /**
     * true to execute a Flyway validate command when the application starts, false otherwise.
     *
     */
    @WithDefault("false")
    boolean validateAtStart();

    /**
     * true to automatically execute a Flyway clean command when a validation error occurs at start, false otherwise.
     */
    @WithName("validate-at-start.clean-on-validation-error")
    @WithDefault("false")
    boolean cleanOnValidationError();

    /**
     * true to execute Flyway baseline before migrations This flag is ignored if the flyway_schema_history table exists in the
     * current schema or if the current schema is empty.
     * Note that this will not automatically call migrate, you must either enable baselineAtStart or programmatically call
     * flyway.migrate().
     */
    @WithDefault("false")
    boolean baselineOnMigrate();

    /**
     * true to execute Flyway baseline automatically when the application starts.
     * This flag is ignored if the flyway_schema_history table exists in the current schema.
     * This will work even if the current schema is empty.
     */
    @WithDefault("false")
    boolean baselineAtStart();

    /**
     * The initial baseline version.
     */
    Optional<String> baselineVersion();

    /**
     * The description to tag an existing schema with when executing baseline.
     */
    Optional<String> baselineDescription();

    /**
     * Whether to automatically call validate when performing a migration.
     */
    @WithDefault("true")
    boolean validateOnMigrate();

    /**
     * Allows migrations to be run "out of order".
     */
    @WithDefault("false")
    boolean outOfOrder();

    /**
     * Ignore missing migrations when reading the history table. When set to true migrations from older versions present in the
     * history table but absent in the configured locations will be ignored (and logged as a warning), when false (the default)
     * the validation step will fail.
     */
    @WithDefault("false")
    boolean ignoreMissingMigrations();

    /**
     * Ignore future migrations when reading the history table. When set to true migrations from newer versions present in the
     * history table but absent in the configured locations will be ignored (and logged as a warning), when false (the default)
     * the validation step will fail.
     */
    @WithDefault("false")
    boolean ignoreFutureMigrations();

    /**
     * Sets the placeholders to replace in SQL migration scripts.
     */
    @ConfigDocMapKey("placeholder-key")
    Map<String, String> placeholders();

    /**
     * Whether Flyway should attempt to create the schemas specified in the schemas property
     */
    @WithDefault("true")
    boolean createSchemas();

    /**
     * Prefix of every placeholder (default: ${ )
     */
    Optional<String> placeholderPrefix();

    /**
     * Suffix of every placeholder (default: } )
     */
    Optional<String> placeholderSuffix();

    /**
     * The SQL statements to run to initialize a new database connection immediately after opening it.
     */
    Optional<String> initSql();

    /**
     * Whether to validate migrations and callbacks whose scripts do not obey the correct naming convention. A failure can be
     * useful to check that errors such as case sensitivity in migration prefixes have been corrected.
     */
    @WithDefault("false")
    boolean validateMigrationNaming();

    /**
     * Ignore migrations during validate and repair according to a given list of patterns (see
     * https://flywaydb.org/documentation/configuration/parameters/ignoreMigrationPatterns for more information).
     * When this configuration is set, the ignoreFutureMigrations and ignoreMissingMigrations settings are ignored. Patterns are
     * comma separated.
     */
    Optional<String[]> ignoreMigrationPatterns();
}
