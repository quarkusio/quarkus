package io.quarkus.flyway.runtime;

import java.util.List;
import java.util.stream.Collectors;

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
        List<String> notEmptySchemas = filterBlanks(flywayRuntimeConfig.schemas);
        if (!notEmptySchemas.isEmpty()) {
            configure.schemas(notEmptySchemas.toArray(new String[0]));
        }
        flywayRuntimeConfig.table.ifPresent(configure::table);
        List<String> notEmptyLocations = filterBlanks(flywayBuildConfig.locations);
        if (!notEmptyLocations.isEmpty()) {
            configure.locations(notEmptyLocations.toArray(new String[0]));
        }
        flywayRuntimeConfig.sqlMigrationPrefix.ifPresent(configure::sqlMigrationPrefix);
        flywayRuntimeConfig.repeatableSqlMigrationPrefix.ifPresent(configure::repeatableSqlMigrationPrefix);
        return configure.load();
    }

    // NOTE: Have to do this filtering because SmallRye config was injecting an empty string in the list somehow!
    // TODO: remove this when https://github.com/quarkusio/quarkus/issues/2288 is fixed
    private List<String> filterBlanks(List<String> values) {
        return values.stream().filter(it -> it != null && !"".equals(it))
                .collect(Collectors.toList());
    }

    public void setFlywayRuntimeConfig(FlywayRuntimeConfig flywayRuntimeConfig) {
        this.flywayRuntimeConfig = flywayRuntimeConfig;
    }

    public void setFlywayBuildConfig(FlywayBuildConfig flywayBuildConfig) {
        this.flywayBuildConfig = flywayBuildConfig;
    }
}
