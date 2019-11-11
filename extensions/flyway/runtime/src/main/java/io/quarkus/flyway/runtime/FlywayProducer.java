package io.quarkus.flyway.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import io.agroal.api.AgroalDataSource;

@ApplicationScoped
public class FlywayProducer {
    @Inject
    AgroalDataSource dataSource;
    private FlywayRuntimeConfig flywayRuntimeConfig;
    private FlywayBuildConfig flywayBuildConfig;

    @Produces
    @Dependent
    public Flyway produceFlyway() {
        FluentConfiguration configure = Flyway.configure();
        configure.dataSource(dataSource);
        flywayRuntimeConfig.connectRetries.ifPresent(configure::connectRetries);
        flywayRuntimeConfig.schemas.ifPresent(l -> configure.schemas(l.toArray(new String[0])));
        flywayRuntimeConfig.table.ifPresent(configure::table);
        flywayBuildConfig.locations.ifPresent(l -> configure.locations(l.toArray(new String[0])));
        flywayRuntimeConfig.sqlMigrationPrefix.ifPresent(configure::sqlMigrationPrefix);
        flywayRuntimeConfig.repeatableSqlMigrationPrefix.ifPresent(configure::repeatableSqlMigrationPrefix);

        configure.baselineOnMigrate(flywayRuntimeConfig.baselineOnMigrate);
        flywayRuntimeConfig.baselineVersion.ifPresent(configure::baselineVersion);
        flywayRuntimeConfig.baselineDescription.ifPresent(configure::baselineDescription);

        return configure.load();
    }

    public void setFlywayRuntimeConfig(FlywayRuntimeConfig flywayRuntimeConfig) {
        this.flywayRuntimeConfig = flywayRuntimeConfig;
    }

    public void setFlywayBuildConfig(FlywayBuildConfig flywayBuildConfig) {
        this.flywayBuildConfig = flywayBuildConfig;
    }
}
