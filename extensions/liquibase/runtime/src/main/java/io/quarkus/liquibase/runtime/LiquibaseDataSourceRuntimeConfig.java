package io.quarkus.liquibase.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * The liquibase data source runtime time configuration
 */
@ConfigGroup
public final class LiquibaseDataSourceRuntimeConfig {

    /**
     * The default liquibase lock table
     */
    static final String DEFAULT_LOCK_TABLE = "DATABASECHANGELOGLOCK";

    /**
     * The default liquibase log table
     */
    static final String DEFAULT_LOG_TABLE = "DATABASECHANGELOG";

    /**
     * Creates a {@link LiquibaseDataSourceRuntimeConfig} with default settings.
     *
     * @return {@link LiquibaseDataSourceRuntimeConfig}
     */
    public static final LiquibaseDataSourceRuntimeConfig defaultConfig() {
        LiquibaseDataSourceRuntimeConfig config = new LiquibaseDataSourceRuntimeConfig();
        config.databaseChangeLogLockTableName = Optional.of(DEFAULT_LOCK_TABLE);
        config.databaseChangeLogTableName = Optional.of(DEFAULT_LOG_TABLE);
        return config;
    }

    /**
     * {@code true} to execute Liquibase automatically when the application starts, {@code false} otherwise.
     *
     */
    @ConfigItem
    public boolean migrateAtStart;

    /**
     * {@code true} to validate the applied changes against the available ones, {@code false} otherwise. It is only used if
     * {@code migration-at-start} is {@code true}
     *
     */
    @ConfigItem(defaultValue = "true")
    public boolean validateOnMigrate;

    /**
     * {@code true} to execute Liquibase clean command automatically when the application starts, {@code false} otherwise.
     *
     */
    @ConfigItem
    public boolean cleanAtStart;

    /**
     * Comma-separated case-sensitive list of ChangeSet contexts to execute for liquibase.
     */
    @ConfigItem
    public Optional<List<String>> contexts = Optional.empty();

    /**
     * Comma-separated case-sensitive list of expressions defining labeled ChangeSet to execute for liquibase.
     */
    @ConfigItem
    public Optional<List<String>> labels = Optional.empty();

    /**
     * Map of parameters that can be used inside Liquibase changeLog files.
     */
    @ConfigItem
    public Map<String, String> changeLogParameters = new HashMap<>();

    /**
     * The liquibase change log lock table name. Name of table to use for tracking concurrent Liquibase usage.
     */
    @ConfigItem(defaultValue = DEFAULT_LOCK_TABLE)
    public Optional<String> databaseChangeLogLockTableName = Optional.empty();

    /**
     * The liquibase change log table name. Name of table to use for tracking change history.
     */
    @ConfigItem(defaultValue = DEFAULT_LOG_TABLE)
    public Optional<String> databaseChangeLogTableName = Optional.empty();

    /**
     * The name of Liquibase's default catalog.
     */
    @ConfigItem
    public Optional<String> defaultCatalogName = Optional.empty();

    /**
     * The name of Liquibase's default schema. Overwrites the default schema name
     * (returned by the RDBMS) with a different database schema.
     */
    @ConfigItem
    public Optional<String> defaultSchemaName = Optional.empty();

    /**
     * The name of the catalog with the liquibase tables.
     */
    @ConfigItem
    public Optional<String> liquibaseCatalogName = Optional.empty();

    /**
     * The name of the schema with the liquibase tables.
     */
    @ConfigItem
    public Optional<String> liquibaseSchemaName = Optional.empty();

    /**
     * The name of the tablespace where the -LOG and -LOCK tables will be created (if they do not exist yet).
     */
    @ConfigItem
    public Optional<String> liquibaseTablespaceName = Optional.empty();

}
