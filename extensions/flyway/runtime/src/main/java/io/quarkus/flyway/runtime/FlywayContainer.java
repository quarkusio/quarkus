package io.quarkus.flyway.runtime;

import org.flywaydb.core.Flyway;

public class FlywayContainer {

    private final Flyway flyway;
    private final boolean cleanAtStart;
    private final boolean migrateAtStart;

    public FlywayContainer(Flyway flyway, boolean cleanAtStart, boolean migrateAtStart) {
        this.flyway = flyway;
        this.cleanAtStart = cleanAtStart;
        this.migrateAtStart = migrateAtStart;
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
}
