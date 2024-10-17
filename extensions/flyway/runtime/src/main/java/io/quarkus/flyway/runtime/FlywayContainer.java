package io.quarkus.flyway.runtime;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;

public class FlywayContainer {

    private final FlywayCreator flyway;

    private final DataSource dataSource;

    private final boolean multiTenancyEnabled;
    private final boolean baselineAtStart;
    private final boolean cleanAtStart;
    private final boolean migrateAtStart;
    private final boolean repairAtStart;

    private final boolean validateAtStart;
    private final String dataSourceName;
    private final boolean hasMigrations;
    private final boolean createPossible;
    private final String id;

    public FlywayContainer(FlywayCreator flyway, DataSource dataSource, boolean baselineAtStart, boolean cleanAtStart,
            boolean migrateAtStart,
            boolean repairAtStart, boolean validateAtStart,
            String dataSourceName, String name, boolean multiTenancyEnabled, boolean hasMigrations, boolean createPossible) {
        this.flyway = flyway;
        this.dataSource = dataSource;
        this.multiTenancyEnabled = multiTenancyEnabled;
        this.baselineAtStart = baselineAtStart;
        this.cleanAtStart = cleanAtStart;
        this.migrateAtStart = migrateAtStart;
        this.repairAtStart = repairAtStart;
        this.validateAtStart = validateAtStart;
        this.dataSourceName = dataSourceName;
        this.hasMigrations = hasMigrations;
        this.createPossible = createPossible;
        this.id = name.replace("<", "").replace(">", "");
    }

    public Flyway getFlyway() {
        return flyway.createFlyway(dataSource);
    }

    public Flyway getFlyway(String tenantId) {
        if (!isMultiTenancyEnabled()) {
            throw new RuntimeException("Multi-tenancy is not enabled");
        }
        return flyway.createFlyway(dataSource, tenantId);
    }

    public boolean isMultiTenancyEnabled() {
        return multiTenancyEnabled;
    }

    public boolean isBaselineAtStart() {
        return baselineAtStart;
    }

    public boolean isCleanAtStart() {
        return cleanAtStart;
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
