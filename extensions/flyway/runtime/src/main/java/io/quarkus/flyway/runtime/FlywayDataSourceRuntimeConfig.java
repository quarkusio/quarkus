package io.quarkus.flyway.runtime;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public final class FlywayDataSourceRuntimeConfig {

    /**
     * Creates a {@link FlywayDataSourceRuntimeConfig} with default settings.
     *
     * @return {@link FlywayDataSourceRuntimeConfig}
     */
    public static FlywayDataSourceRuntimeConfig defaultConfig() {
        return new FlywayDataSourceRuntimeConfig();
    }

    /**
     * Flag to activate/deactivate Flyway for a specific datasource at runtime.
     */
    @ConfigItem(defaultValueDocumentation = "'true' if the datasource is active; 'false' otherwise")
    public Optional<Boolean> active = Optional.empty();

    /**
     * The maximum number of retries when attempting to connect to the database.
     * <p>
     * After each failed attempt, Flyway will wait up to the configured `connect-retries-interval` duration before
     * attempting to connect again, up to the maximum number of times specified by connectRetries.
     */
    @ConfigItem
    public OptionalInt connectRetries = OptionalInt.empty();

    /**
     * The maximum time between retries when attempting to connect to the database.
     * <p>
     * This will cap the interval between connect retries to the value provided.
     */
    @ConfigItem(defaultValueDocumentation = "120 seconds")
    public Optional<Duration> connectRetriesInterval = Optional.empty();

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
    @ConfigItem
    public Optional<String> defaultSchema = Optional.empty();

    /**
     * The JDBC URL that Flyway uses to connect to the database.
     * Falls back to the datasource URL if not specified.
     */
    @ConfigItem
    public Optional<String> jdbcUrl = Optional.empty();

    /**
     * The username that Flyway uses to connect to the database.
     * If no specific JDBC URL is configured, falls back to the datasource username if not specified.
     */
    @ConfigItem
    public Optional<String> username = Optional.empty();

    /**
     * The password that Flyway uses to connect to the database.
     * If no specific JDBC URL is configured, falls back to the datasource password if not specified.
     */
    @ConfigItem
    public Optional<String> password = Optional.empty();

    /**
     * Comma-separated case-sensitive list of schemas managed by Flyway.
     * The first schema in the list will be automatically set as the default one during the migration.
     * It will also be the one containing the schema history table.
     */
    @ConfigItem
    public Optional<List<String>> schemas = Optional.empty();

    /**
     * The name of Flyway's schema history table.
     * By default (single-schema mode), the schema history table is placed in the default schema for the connection provided by
     * the datasource.
     * When the flyway.schemas property is set (multi-schema mode), the schema history table is placed in the first schema of
     * the list.
     */
    @ConfigItem
    public Optional<String> table = Optional.empty();

    /**
     * The file name prefix for versioned SQL migrations.
     *
     * Versioned SQL migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix , which using
     * the defaults translates to V1.1__My_description.sql
     */
    @ConfigItem
    public Optional<String> sqlMigrationPrefix = Optional.empty();

    /**
     * The file name prefix for repeatable SQL migrations.
     *
     * Repeatable SQL migrations have the following file name structure: prefixSeparatorDESCRIPTIONsuffix , which using the
     * defaults translates to R__My_description.sql
     */
    @ConfigItem
    public Optional<String> repeatableSqlMigrationPrefix = Optional.empty();

    /**
     * true to execute Flyway clean command automatically when the application starts, false otherwise.
     *
     */
    @ConfigItem
    public boolean cleanAtStart;

    /**
     * true to prevent Flyway clean operations, false otherwise.
     */
    @ConfigItem
    public boolean cleanDisabled;

    /**
     * true to automatically call clean when a validation error occurs, false otherwise.
     */
    @ConfigItem
    public boolean cleanOnValidationError;

    /**
     * true to execute Flyway automatically when the application starts, false otherwise.
     *
     */
    @ConfigItem
    public boolean migrateAtStart;

    /**
     * true to execute a Flyway repair command when the application starts, false otherwise.
     *
     */
    @ConfigItem
    public boolean repairAtStart;

    /**
     * true to execute a Flyway validate command when the application starts, false otherwise.
     *
     */
    @ConfigItem
    public boolean validateAtStart;

    /**
     * true to execute Flyway baseline before migrations This flag is ignored if the flyway_schema_history table exists in the
     * current schema or if the current schema is empty.
     * Note that this will not automatically call migrate, you must either enable baselineAtStart or programmatically call
     * flyway.migrate().
     */
    @ConfigItem
    public boolean baselineOnMigrate;

    /**
     * true to execute Flyway baseline automatically when the application starts.
     * This flag is ignored if the flyway_schema_history table exists in the current schema.
     * This will work even if the current schema is empty.
     */
    @ConfigItem
    public boolean baselineAtStart;

    /**
     * The initial baseline version.
     */
    @ConfigItem
    public Optional<String> baselineVersion = Optional.empty();

    /**
     * The description to tag an existing schema with when executing baseline.
     */
    @ConfigItem
    public Optional<String> baselineDescription = Optional.empty();

    /**
     * Whether to automatically call validate when performing a migration.
     */
    @ConfigItem(defaultValue = "true")
    public boolean validateOnMigrate = true;

    /**
     * Allows migrations to be run "out of order".
     */
    @ConfigItem
    public boolean outOfOrder;

    /**
     * Ignore missing migrations when reading the history table. When set to true migrations from older versions present in the
     * history table but absent in the configured locations will be ignored (and logged as a warning), when false (the default)
     * the validation step will fail.
     */
    @ConfigItem
    public boolean ignoreMissingMigrations;

    /**
     * Ignore future migrations when reading the history table. When set to true migrations from newer versions present in the
     * history table but absent in the configured locations will be ignored (and logged as a warning), when false (the default)
     * the validation step will fail.
     */
    @ConfigItem
    public boolean ignoreFutureMigrations;

    /**
     * Sets the placeholders to replace in SQL migration scripts.
     */
    @ConfigItem
    public Map<String, String> placeholders = Collections.emptyMap();

    /**
     * Whether Flyway should attempt to create the schemas specified in the schemas property
     */
    @ConfigItem(defaultValue = "true")
    public boolean createSchemas;

    /**
     * Prefix of every placeholder (default: ${ )
     */
    @ConfigItem
    public Optional<String> placeholderPrefix = Optional.empty();

    /**
     * Suffix of every placeholder (default: } )
     */
    @ConfigItem
    public Optional<String> placeholderSuffix = Optional.empty();

    /**
     * The SQL statements to run to initialize a new database connection immediately after opening it.
     */
    @ConfigItem
    public Optional<String> initSql = Optional.empty();

    /**
     * Whether to validate migrations and callbacks whose scripts do not obey the correct naming convention. A failure can be
     * useful to check that errors such as case sensitivity in migration prefixes have been corrected.
     */
    @ConfigItem
    public boolean validateMigrationNaming;

    /**
     * Ignore migrations during validate and repair according to a given list of patterns (see
     * https://flywaydb.org/documentation/configuration/parameters/ignoreMigrationPatterns for more information).
     * When this configuration is set, the ignoreFutureMigrations and ignoreMissingMigrations settings are ignored. Patterns are
     * comma separated.
     */
    @ConfigItem
    public Optional<String[]> ignoreMigrationPatterns = Optional.empty();
}
