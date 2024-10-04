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
        config.active = liquibaseBuildTimeConfig.active;
        config.changeLog = liquibaseBuildTimeConfig.changeLog;
        config.searchPath = liquibaseBuildTimeConfig.searchPath;
        config.changeLogParameters = liquibaseRuntimeConfig.changeLogParameters;

        liquibaseRuntimeConfig.labels.ifPresent(lbls -> config.labels = lbls);
        liquibaseRuntimeConfig.contexts.ifPresent(ctxs -> config.contexts = ctxs);
        liquibaseRuntimeConfig.databaseChangeLogLockTableName
                .ifPresent(name -> config.databaseChangeLogLockTableName = name);
        liquibaseRuntimeConfig.databaseChangeLogTableName.ifPresent(name -> config.databaseChangeLogTableName = name);

        config.password = liquibaseRuntimeConfig.password;
        config.username = liquibaseRuntimeConfig.username;
        config.defaultSchemaName = liquibaseRuntimeConfig.defaultSchemaName;
        config.defaultCatalogName = liquibaseRuntimeConfig.defaultCatalogName;
        config.liquibaseTablespaceName = liquibaseRuntimeConfig.liquibaseTablespaceName;
        config.liquibaseSchemaName = liquibaseRuntimeConfig.liquibaseSchemaName;
        config.liquibaseCatalogName = liquibaseRuntimeConfig.liquibaseCatalogName;
        config.migrateAtStart = liquibaseRuntimeConfig.migrateAtStart;
        config.cleanAtStart = liquibaseRuntimeConfig.cleanAtStart;
        config.validateOnMigrate = liquibaseRuntimeConfig.validateOnMigrate;
        config.allowDuplicatedChangesetIdentifiers = liquibaseRuntimeConfig.allowDuplicatedChangesetIdentifiers;
        return new LiquibaseFactory(config, dataSource, dataSourceName);
    }
}
