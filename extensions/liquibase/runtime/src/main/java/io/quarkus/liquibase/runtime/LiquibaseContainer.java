package io.quarkus.liquibase.runtime;

import io.quarkus.liquibase.LiquibaseFactory;

public class LiquibaseContainer {

    private final LiquibaseFactory liquibaseFactory;
    private final boolean cleanAtStart;
    private final boolean migrateAtStart;
    private final boolean validateOnMigrate;
    private final String dataSourceName;

    public LiquibaseContainer(LiquibaseFactory liquibaseFactory, boolean cleanAtStart, boolean migrateAtStart,
            boolean validateOnMigrate, String dataSourceName) {
        this.liquibaseFactory = liquibaseFactory;
        this.cleanAtStart = cleanAtStart;
        this.migrateAtStart = migrateAtStart;
        this.validateOnMigrate = validateOnMigrate;
        this.dataSourceName = dataSourceName;
    }

    public LiquibaseFactory getLiquibaseFactory() {
        return liquibaseFactory;
    }

    public boolean isCleanAtStart() {
        return cleanAtStart;
    }

    public boolean isMigrateAtStart() {
        return migrateAtStart;
    }

    public boolean isValidateOnMigrate() {
        return validateOnMigrate;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public String getEffectiveDataSourceName() {
        if (io.quarkus.datasource.common.runtime.DataSourceUtil.isDefault(dataSourceName)) {
            return "default";
        }
        return dataSourceName;
    }
}
