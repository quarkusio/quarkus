package io.quarkus.flyway.mongodb.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Per-client runtime configuration.
 */
@ConfigGroup
public interface FlywayMongodbClientRuntimeConfig {

    /**
     * Flag to activate/deactivate Flyway for a specific MongoDB client at runtime.
     */
    @ConfigDocDefault("'true' if the MongoDB client is active; 'false' otherwise")
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
     * Optional override for the connection string Flyway uses.
     * Defaults to the corresponding {@code quarkus.mongodb.<client>.connection-string}.
     */
    Optional<String> connectionString();

    /**
     * Target database for migrations.
     * Defaults to the corresponding {@code quarkus.mongodb.<client>.database}.
     */
    Optional<String> database();

    /**
     * Schema history collection name.
     */
    Optional<String> collection();

    /**
     * The file name prefix for versioned migrations.
     * Versioned migrations have the following file name structure:
     * prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults translates to V1.1__My_description.js
     */
    Optional<String> migrationPrefix();

    /**
     * The file name prefix for repeatable migrations.
     * Repeatable migrations have the following file name structure:
     * prefixSeparatorDESCRIPTIONsuffix, which using the defaults translates to R__My_description.js
     */
    Optional<String> repeatableMigrationPrefix();

    /**
     * true to execute Flyway clean command automatically when the application starts, false otherwise.
     *
     */
    @ConfigDocDefault("`false`")
    Optional<Boolean> cleanAtStart();

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
     * true to execute Flyway baseline before migrations. This flag is ignored if the flyway_schema_history collection already
     * exists.
     * Note that this will not automatically call migrate, you must either enable baselineAtStart or programmatically call
     * flyway.migrate().
     */
    @WithDefault("false")
    boolean baselineOnMigrate();

    /**
     * true to execute Flyway baseline automatically when the application starts.
     * This flag is ignored if the flyway_schema_history collection already exists.
     * Note: this does not also call migrate; pair with {@code migrate-at-start} if needed.
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
     * Sets the placeholders to replace in migration scripts.
     */
    @ConfigDocMapKey("placeholder-key")
    Map<String, String> placeholders();

    /**
     * Prefix of every placeholder (default: ${ )
     */
    Optional<String> placeholderPrefix();

    /**
     * Suffix of every placeholder (default: } )
     */
    Optional<String> placeholderSuffix();

    /**
     * Whether to validate migrations and callbacks whose scripts do not obey the correct naming convention. A failure can be
     * useful to check that errors such as case sensitivity in migration prefixes have been corrected.
     */
    @WithDefault("false")
    boolean validateMigrationNaming();

    /**
     * Ignore migrations during validate and repair according to a given list of patterns.
     * When this configuration is set, the ignoreFutureMigrations and ignoreMissingMigrations settings are ignored. Patterns are
     * comma separated.
     */
    Optional<String[]> ignoreMigrationPatterns();
}
