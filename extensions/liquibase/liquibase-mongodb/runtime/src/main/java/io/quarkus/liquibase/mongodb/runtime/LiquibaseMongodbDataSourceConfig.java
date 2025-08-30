package io.quarkus.liquibase.mongodb.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * The liquibase data source runtime configuration
 */
@ConfigGroup
public interface LiquibaseMongodbDataSourceConfig {
    /**
     * Mongodb client name to use to connect to database, defaults to the default mongodb client.
     */
    Optional<String> mongoClientName();

    /**
     * The migrate at start flag
     */
    @WithDefault("false")
    boolean migrateAtStart();

    /**
     * The validate on update flag
     */
    @WithDefault("true")
    boolean validateOnMigrate();

    /**
     * The clean at start flag
     */
    @WithDefault("false")
    boolean cleanAtStart();

    /**
     * The parameters to be passed to the changelog.
     * Defined as key value pairs.
     */
    Map<String, String> changeLogParameters();

    /**
     * The list of contexts
     */
    Optional<List<String>> contexts();

    /**
     * The list of labels
     */
    Optional<List<String>> labels();

    /**
     * The default catalog name
     */
    Optional<String> defaultCatalogName();

    /**
     * The default schema name
     */
    Optional<String> defaultSchemaName();

    /**
     * The liquibase tables catalog name
     */
    Optional<String> liquibaseCatalogName();

    /**
     * The liquibase tables schema name
     */
    Optional<String> liquibaseSchemaName();

    /**
     * The liquibase tables tablespace name
     */
    Optional<String> liquibaseTablespaceName();
}
