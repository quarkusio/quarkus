package io.quarkus.flyway.runtime;

import org.flywaydb.core.Flyway;

public class FlywayContainer {

    private final Flyway flyway;

    private final boolean baselineAtStart;
    private final boolean cleanAtStart;
    private final boolean cleanOnValidationError;
    private final boolean migrateAtStart;
    private final boolean repairAtStart;

    private final boolean validateAtStart;
    private final String dataSourceName;
    private final boolean hasMigrations;
    private final boolean createPossible;
    private final String id;

    public FlywayContainer(Flyway flyway, boolean baselineAtStart, boolean cleanAtStart, boolean cleanOnValidationError,
            boolean migrateAtStart, boolean repairAtStart, boolean validateAtStart,
            String dataSourceName, boolean hasMigrations, boolean createPossible) {
        this.flyway = flyway;
        this.baselineAtStart = baselineAtStart;
        this.cleanAtStart = cleanAtStart;
        this.cleanOnValidationError = cleanOnValidationError;
        this.migrateAtStart = migrateAtStart;
        this.repairAtStart = repairAtStart;
        this.validateAtStart = validateAtStart;
        this.dataSourceName = dataSourceName;
        this.hasMigrations = hasMigrations;
        this.createPossible = createPossible;
        this.id = dataSourceName.replace("<", "").replace(">", "");
    }

    public Flyway getFlyway() {
        return flyway;
    }

    public boolean isBaselineAtStart() {
        return baselineAtStart;
    }

    public boolean isCleanAtStart() {
        return cleanAtStart;
    }

    public boolean isCleanOnValidationError() {
        return cleanOnValidationError;
    }

    public boolean isMigrateAtStart() {
        return migrateAtStart;
    }

    public boolean isRepairAtStart() {
        return repairAtStart;
    }

    public boolean isValidateAtStart() {
        return validateAtStart;
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
