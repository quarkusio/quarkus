package io.quarkus.flyway.runtime;

import org.flywaydb.core.Flyway;

public class FlywayContainer {

    private final Flyway flyway;
    private final boolean cleanAtStart;
    private final boolean migrateAtStart;
    private final String dataSourceName;
    private final boolean hasMigrations;
    private final boolean createPossible;

    public FlywayContainer(Flyway flyway, boolean cleanAtStart, boolean migrateAtStart, String dataSourceName,
            boolean hasMigrations, boolean createPossible) {
        this.flyway = flyway;
        this.cleanAtStart = cleanAtStart;
        this.migrateAtStart = migrateAtStart;
        this.dataSourceName = dataSourceName;
        this.hasMigrations = hasMigrations;
        this.createPossible = createPossible;
    }

    public Flyway getFlyway() {
        return flyway;
    }

    public boolean isCleanAtStart() {
        return cleanAtStart;
    }

    public boolean isMigrateAtStart() {
        return migrateAtStart;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public boolean isHasMigrations() {
        return hasMigrations;
    }

    public boolean isCreatePossible() {
        return createPossible;
    }
}
