package io.quarkus.flyway.runtime;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

class FlywayCreator {
    private final FlywayDataSourceRuntimeConfig flywayRuntimeConfig;
    private final FlywayDataSourceBuildConfig flywayBuildConfig;

    public FlywayCreator(FlywayDataSourceRuntimeConfig flywayRuntimeConfig, FlywayDataSourceBuildConfig flywayBuildConfig) {
        this.flywayRuntimeConfig = flywayRuntimeConfig;
        this.flywayBuildConfig = flywayBuildConfig;
    }

    public Flyway createFlyway(DataSource dataSource) {
        FluentConfiguration configure = Flyway.configure();
        configure.dataSource(dataSource);
        flywayRuntimeConfig.connectRetries.ifPresent(configure::connectRetries);
        flywayRuntimeConfig.schemas.ifPresent(list -> configure.schemas(list.toArray(new String[0])));
        flywayRuntimeConfig.table.ifPresent(configure::table);
        flywayBuildConfig.locations.ifPresent(list -> configure.locations(list.toArray(new String[0])));
        flywayRuntimeConfig.sqlMigrationPrefix.ifPresent(configure::sqlMigrationPrefix);
        flywayRuntimeConfig.repeatableSqlMigrationPrefix.ifPresent(configure::repeatableSqlMigrationPrefix);

        configure.baselineOnMigrate(flywayRuntimeConfig.baselineOnMigrate);
        flywayRuntimeConfig.baselineVersion.ifPresent(configure::baselineVersion);
        flywayRuntimeConfig.baselineDescription.ifPresent(configure::baselineDescription);

        return configure.load();
    }
}