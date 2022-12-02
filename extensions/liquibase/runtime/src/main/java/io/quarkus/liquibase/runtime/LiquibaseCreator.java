package io.quarkus.liquibase.runtime;

import javax.sql.DataSource;

import io.quarkus.liquibase.LiquibaseFactory;

class LiquibaseCreator {

    private final LiquibaseDataSourceRuntimeConfig liquibaseRuntimeConfig;
    private final LiquibaseDataSourceBuildTimeConfig liquibaseBuildTimeConfig;

    public LiquibaseCreator(LiquibaseDataSourceRuntimeConfig liquibaseRuntimeConfig,
            LiquibaseDataSourceBuildTimeConfig liquibaseBuildTimeConfig) {
        this.liquibaseRuntimeConfig = liquibaseRuntimeConfig;
        this.liquibaseBuildTimeConfig = liquibaseBuildTimeConfig;
    }

    public LiquibaseFactory createLiquibaseFactory(DataSource dataSource, String dataSourceName) {
        LiquibaseConfig config = new LiquibaseConfig();
        config.changeLog = liquibaseBuildTimeConfig.changeLog;
        config.changeLogParameters = liquibaseRuntimeConfig.changeLogParameters;

        if (liquibaseRuntimeConfig.labels.isPresent()) {
            config.labels = liquibaseRuntimeConfig.labels.get();
        }
        if (liquibaseRuntimeConfig.contexts.isPresent()) {
            config.contexts = liquibaseRuntimeConfig.contexts.get();
        }
        if (liquibaseRuntimeConfig.databaseChangeLogLockTableName.isPresent()) {
            config.databaseChangeLogLockTableName = liquibaseRuntimeConfig.databaseChangeLogLockTableName.get();
        }
        if (liquibaseRuntimeConfig.databaseChangeLogTableName.isPresent()) {
            config.databaseChangeLogTableName = liquibaseRuntimeConfig.databaseChangeLogTableName.get();
        }
        config.defaultSchemaName = liquibaseRuntimeConfig.defaultSchemaName;
        config.defaultCatalogName = liquibaseRuntimeConfig.defaultCatalogName;
        config.liquibaseTablespaceName = liquibaseRuntimeConfig.liquibaseTablespaceName;
        config.liquibaseSchemaName = liquibaseRuntimeConfig.liquibaseSchemaName;
        config.liquibaseCatalogName = liquibaseRuntimeConfig.liquibaseCatalogName;
        config.migrateAtStart = liquibaseRuntimeConfig.migrateAtStart;
        config.cleanAtStart = liquibaseRuntimeConfig.cleanAtStart;
        config.validateOnMigrate = liquibaseRuntimeConfig.validateOnMigrate;
        return new LiquibaseFactory(config, dataSource, dataSourceName);
    }
}
