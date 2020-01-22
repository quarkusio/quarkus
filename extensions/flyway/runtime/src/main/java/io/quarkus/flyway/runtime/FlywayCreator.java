package io.quarkus.flyway.runtime;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

class FlywayCreator {

    private final FlywayDataSourceRuntimeConfig flywayRuntimeConfig;
    private final FlywayDataSourceBuildTimeConfig flywayBuildTimeConfig;

    public FlywayCreator(FlywayDataSourceRuntimeConfig flywayRuntimeConfig,
            FlywayDataSourceBuildTimeConfig flywayBuildTimeConfig) {
        this.flywayRuntimeConfig = flywayRuntimeConfig;
        this.flywayBuildTimeConfig = flywayBuildTimeConfig;
    }

    public Flyway createFlyway(DataSource dataSource) {
        FluentConfiguration configure = Flyway.configure();
        configure.dataSource(dataSource);
        if (flywayRuntimeConfig.connectRetries.isPresent()) {
            configure.connectRetries(flywayRuntimeConfig.connectRetries.getAsInt());
        }
        if (flywayRuntimeConfig.schemas.isPresent()) {
            configure.schemas(flywayRuntimeConfig.schemas.get());
        }
        if (flywayRuntimeConfig.table.isPresent()) {
            configure.table(flywayRuntimeConfig.table.get());
        }
        if (flywayBuildTimeConfig.locations.isPresent()) {
            configure.locations(flywayBuildTimeConfig.locations.get());
        }
        if (flywayRuntimeConfig.sqlMigrationPrefix.isPresent()) {
            configure.sqlMigrationPrefix(flywayRuntimeConfig.sqlMigrationPrefix.get());
        }
        if (flywayRuntimeConfig.repeatableSqlMigrationPrefix.isPresent()) {
            configure.repeatableSqlMigrationPrefix(flywayRuntimeConfig.repeatableSqlMigrationPrefix.get());
        }
        if (flywayRuntimeConfig.baselineVersion.isPresent()) {
            configure.baselineVersion(flywayRuntimeConfig.baselineVersion.get());
        }
        if (flywayRuntimeConfig.baselineDescription.isPresent()) {
            configure.baselineDescription(flywayRuntimeConfig.baselineDescription.get());
        }
        configure.baselineOnMigrate(flywayRuntimeConfig.baselineOnMigrate);
        configure.validateOnMigrate(flywayRuntimeConfig.validateOnMigrate);
        return configure.load();
    }
}
