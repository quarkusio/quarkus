package io.quarkus.flyway.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;

@ApplicationScoped
public class FlywayProducer {
    private static final String ERROR_NOT_READY = "The Flyway settings are not ready to be consumed: the %s configuration has not been injected yet";

    @Inject
    @Default
    Instance<DataSource> defaultDataSource;

    @Inject
    FlywayRuntimeConfig flywayRuntimeConfig;

    @Inject
    FlywayBuildTimeConfig flywayBuildConfig;

    @Produces
    @Dependent
    @Default
    public Flyway produceFlyway() {
        return createDefaultFlyway(defaultDataSource.get());
    }

    private Flyway createDefaultFlyway(DataSource dataSource) {
        return new FlywayCreator(getFlywayRuntimeConfig().defaultDataSource, getFlywayBuildConfig().defaultDataSource)
                .createFlyway(dataSource);
    }

    public Flyway createFlyway(DataSource dataSource, String dataSourceName) {
        return new FlywayCreator(getFlywayRuntimeConfig().getConfigForDataSourceName(dataSourceName),
                getFlywayBuildConfig().getConfigForDataSourceName(dataSourceName))
                        .createFlyway(dataSource);
    }

    private FlywayRuntimeConfig getFlywayRuntimeConfig() {
        return failIfNotReady(flywayRuntimeConfig, "runtime");
    }

    private FlywayBuildTimeConfig getFlywayBuildConfig() {
        return failIfNotReady(flywayBuildConfig, "build");
    }

    private static <T> T failIfNotReady(T config, String name) {
        if (config == null) {
            throw new IllegalStateException(String.format(ERROR_NOT_READY, name));
        }
        return config;
    }
}