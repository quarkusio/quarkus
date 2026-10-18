package io.quarkus.liquibase.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * The liquibase data source runtime time configuration
 */
@ConfigGroup
public interface LiquibaseDataSourceRuntimeConfig {

    /**
     * The default liquibase lock table
     */
    static final String DEFAULT_LOCK_TABLE = "DATABASECHANGELOGLOCK";

    /**
     * The default liquibase log table
     */
    static final String DEFAULT_LOG_TABLE = "DATABASECHANGELOG";

    /**
     * {@code true} to execute Liquibase automatically when the application starts, {@code false} otherwise.
     *
     */
    @WithDefault("false")
    boolean migrateAtStart();

    /**
     * {@code true} to validate the applied changes against the available ones, {@code false} otherwise. It is only used if
     * {@code migration-at-start} is {@code true}
     *
     */
    @WithDefault("true")
    boolean validateOnMigrate();

    /**
     * {@code true} to execute Liquibase clean command automatically when the application starts, {@code false} otherwise.
     *
     */
    @WithDefault("false")
    boolean cleanAtStart();

    /**
     * Comma-separated case-sensitive list of ChangeSet contexts to execute for liquibase.
     */
    Optional<List<String>> contexts();

    /**
     * Comma-separated case-sensitive list of expressions defining labeled ChangeSet to execute for liquibase.
     */
    Optional<List<String>> labels();

    /**
     * Map of parameters that can be used inside Liquibase changeLog files.
     */
    @ConfigDocMapKey("parameter-name")
    Map<String, String> changeLogParameters();

    /**
     * The liquibase change log lock table name. Name of table to use for tracking concurrent Liquibase usage.
     */
    @WithDefault(DEFAULT_LOCK_TABLE)
    String databaseChangeLogLockTableName();

    /**
     * The liquibase change log table name. Name of table to use for tracking change history.
     */
    @WithDefault(DEFAULT_LOG_TABLE)
    String databaseChangeLogTableName();

    /**
     * The name of Liquibase's default catalog.
     */
    Optional<String> defaultCatalogName();

    /**
     * The name of Liquibase's default schema. Overwrites the default schema name
     * (returned by the RDBMS) with a different database schema.
     */
    Optional<String> defaultSchemaName();

    /**
     * The username that Liquibase uses to connect to the database.
     * If no specific username is configured, falls back to the datasource username and password.
     */
    Optional<String> username();

    /**
     * The password that Liquibase uses to connect to the database.
     * If no specific password is configured, falls back to the datasource username and password.
     */
    Optional<String> password();

    /**
     * The name of the catalog with the liquibase tables.
     */
    Optional<String> liquibaseCatalogName();

    /**
     * The name of the schema with the liquibase tables.
     */
    Optional<String> liquibaseSchemaName();

    /**
     * The name of the tablespace where the -LOG and -LOCK tables will be created (if they do not exist yet).
     */
    Optional<String> liquibaseTablespaceName();

    /**
     * Allows duplicated changeset identifiers without failing Liquibase execution.
     */
    Optional<Boolean> allowDuplicatedChangesetIdentifiers();

    /**
     * Whether Liquibase should enforce secure parsing.
     * <p>
     * If secure parsing is enforced, insecure files may not be parsed.
     */
    @WithDefault("true")
    boolean secureParsing();
}
