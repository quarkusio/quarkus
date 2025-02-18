package io.quarkus.liquibase.mongodb.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * The liquibase configuration
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.liquibase-mongodb")
public interface LiquibaseMongodbConfig {

    /**
     * Flag to enable / disable Liquibase.
     *
     */
    @WithDefault("true")
    boolean enabled();

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
