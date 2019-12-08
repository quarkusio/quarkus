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
        flywayRuntimeConfig.connectRetries.ifPresent(configure::connectRetries);
        flywayRuntimeConfig.schemas.ifPresent(list -> configure.schemas(list.toArray(new String[0])));
        flywayRuntimeConfig.table.ifPresent(configure::table);
        configure.locations(flywayBuildTimeConfig.locations.toArray(new String[0]));
        flywayRuntimeConfig.sqlMigrationPrefix.ifPresent(configure::sqlMigrationPrefix);
        flywayRuntimeConfig.repeatableSqlMigrationPrefix.ifPresent(configure::repeatableSqlMigrationPrefix);

        configure.baselineOnMigrate(flywayRuntimeConfig.baselineOnMigrate);
        flywayRuntimeConfig.baselineVersion.ifPresent(configure::baselineVersion);
        flywayRuntimeConfig.baselineDescription.ifPresent(configure::baselineDescription);

        return configure.load();
    }
}