package io.quarkus.flyway.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface FlywayDataSourceRuntimeConfig {

    /**
     * The maximum number of retries when attempting to connect to the database. After each failed attempt, Flyway will wait 1
     * second before attempting to connect again, up to the maximum number of times specified by connectRetries.
     */
    OptionalInt connectRetries();

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
     * <p>
     * Versioned SQL migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix , which using
     * the defaults translates to V1.1__My_description.sql
     */
    Optional<String> sqlMigrationPrefix();

    /**
     * The file name prefix for repeatable SQL migrations.
     * <p>
     * Repeatable SQL migrations have the following file name structure: prefixSeparatorDESCRIPTIONsuffix , which using the
     * defaults translates to R__My_description.sql
     */
    Optional<String> repeatableSqlMigrationPrefix();

    /**
     * true to execute Flyway clean command automatically when the application starts, false otherwise.
     */
    @WithDefault("false")
    boolean cleanAtStart();

    /**
     * true to prevent Flyway clean operations, false otherwise.
     */
    @WithDefault("false")
    boolean cleanDisabled();

    /**
     * true to automatically call clean when a validation error occurs, false otherwise.
     */
    @WithDefault("false")
    boolean cleanOnValidationError();

    /**
     * true to execute Flyway automatically when the application starts, false otherwise.
     */
    @WithDefault("false")
    boolean migrateAtStart();

    /**
     * true to execute a Flyway repair command when the application starts, false otherwise.
     */
    @WithDefault("false")
    boolean repairAtStart();

    /**
     * true to execute a Flyway validate command when the application starts, false otherwise.
     */
    @WithDefault("false")
    boolean validateAtStart();

    /**
     * Enable the creation of the history table if it does not exist already.
     */
    @WithDefault("false")
    boolean baselineOnMigrate();

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
