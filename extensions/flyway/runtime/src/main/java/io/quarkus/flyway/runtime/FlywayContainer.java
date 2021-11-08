package io.quarkus.flyway.runtime;

import org.flywaydb.core.Flyway;

public class FlywayContainer {

    private final Flyway flyway;
    private final boolean cleanAtStart;
    private final boolean migrateAtStart;
    private final String dataSourceName;
    private final boolean hasMigrations;
    private final boolean createPossible;
    private final String id;

    public FlywayContainer(Flyway flyway, boolean cleanAtStart, boolean migrateAtStart, String dataSourceName,
            boolean hasMigrations, boolean createPossible) {
        this.flyway = flyway;
        this.cleanAtStart = cleanAtStart;
        this.migrateAtStart = migrateAtStart;
        this.dataSourceName = dataSourceName;
        this.hasMigrations = hasMigrations;
        this.createPossible = createPossible;
        this.id = dataSourceName.replace("<", "").replace(">", "");
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

    public String getId() {
        return this.id;
    }
}
