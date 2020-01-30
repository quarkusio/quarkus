package io.quarkus.flyway.runtime;

import java.util.List;
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
     * Comma-separated case-sensitive list of schemas managed by Flyway.
     * The first schema in the list will be automatically set as the default one during the migration.
     * It will also be the one containing the schema history table.
     */
    @ConfigItem
    public Optional<List<String>> schemas = Optional.empty();

    /**
     * The name of Flyway's schema history table.
     * By default (single-schema mode) the schema history table is placed in the default schema for the connection provided by
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
}
