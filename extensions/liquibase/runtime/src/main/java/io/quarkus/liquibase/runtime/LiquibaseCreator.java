package io.quarkus.liquibase.runtime;

import javax.sql.DataSource;

import io.quarkus.liquibase.LiquibaseFactory;

/**
 * The quarkus liquibase creator
 */
class LiquibaseCreator {
    /**
     * The liquibase runtime configuration
     */
    private final LiquibaseDataSourceRuntimeConfig liquibaseRuntimeConfig;
    /**
     * The liquibase build time configuration
     */
    private final LiquibaseDataSourceBuildTimeConfig liquibaseBuildTimeConfig;

    /**
     * The default constructor
     * 
     * @param liquibaseRuntimeConfig the liquibase runtime configuration
     * @param liquibaseBuildTimeConfig the liquibase build time configuration
     */
    public LiquibaseCreator(LiquibaseDataSourceRuntimeConfig liquibaseRuntimeConfig,
            LiquibaseDataSourceBuildTimeConfig liquibaseBuildTimeConfig) {
        this.liquibaseRuntimeConfig = liquibaseRuntimeConfig;
        this.liquibaseBuildTimeConfig = liquibaseBuildTimeConfig;
    }

    /**
     * Creates the quarkus liquibase instance
     * 
     * @param dataSource the liquibase datasource
     * @return the corresponding quarkus liquibase instance.
     */
    public LiquibaseFactory createLiquibase(DataSource dataSource) {
        LiquibaseConfig config = new LiquibaseConfig();
        config.changeLog = liquibaseBuildTimeConfig.changeLog;
        liquibaseRuntimeConfig.labels.ifPresent(c -> config.labels = c);
        liquibaseRuntimeConfig.contexts.ifPresent(c -> config.contexts = c);
        liquibaseRuntimeConfig.databaseChangeLogLockTableName.ifPresent(c -> config.databaseChangeLogLockTableName = c);
        liquibaseRuntimeConfig.databaseChangeLogTableName.ifPresent(c -> config.databaseChangeLogTableName = c);
        config.defaultSchemaName = liquibaseRuntimeConfig.defaultSchemaName;
        config.defaultCatalogName = liquibaseRuntimeConfig.defaultCatalogName;
        config.liquibaseTablespaceName = liquibaseRuntimeConfig.liquibaseTablespaceName;
        config.liquibaseSchemaName = liquibaseRuntimeConfig.liquibaseSchemaName;
        config.liquibaseCatalogName = liquibaseRuntimeConfig.liquibaseCatalogName;
        config.migrateAtStart = liquibaseRuntimeConfig.migrateAtStart;
        config.cleanAtStart = liquibaseRuntimeConfig.cleanAtStart;
        config.validateOnMigrate = liquibaseRuntimeConfig.validateOnMigrate;
        return new LiquibaseFactory(config, dataSource);
    }
}
