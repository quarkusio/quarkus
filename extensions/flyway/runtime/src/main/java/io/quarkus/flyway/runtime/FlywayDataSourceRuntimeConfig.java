package io.quarkus.flyway.runtime;

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
    public static final FlywayDataSourceRuntimeConfig defaultConfig() {
        return new FlywayDataSourceRuntimeConfig();
    }

    /**
     * The maximum number of retries when attempting to connect to the database. After each failed attempt, Flyway will wait 1
     * second before attempting to connect again, up to the maximum number of times specified by connectRetries.
     */
    @ConfigItem
    public OptionalInt connectRetries = OptionalInt.empty();

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
     * Enable the creation of the history table if it does not exist already.
     */
    @ConfigItem
    public boolean baselineOnMigrate;

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
    @ConfigItem
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
    public boolean ignoreFutureMigrations = true;

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
}
