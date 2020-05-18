package io.quarkus.liquibase.runtime;

import io.quarkus.liquibase.LiquibaseFactory;

public class LiquibaseContainer {

    private final LiquibaseFactory liquibaseFactory;
    private final boolean cleanAtStart;
    private final boolean migrateAtStart;
    private final boolean validateOnMigrate;

    public LiquibaseContainer(LiquibaseFactory liquibaseFactory, boolean cleanAtStart, boolean migrateAtStart,
            boolean validateOnMigrate) {
        this.liquibaseFactory = liquibaseFactory;
        this.cleanAtStart = cleanAtStart;
        this.migrateAtStart = migrateAtStart;
        this.validateOnMigrate = validateOnMigrate;
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
}
