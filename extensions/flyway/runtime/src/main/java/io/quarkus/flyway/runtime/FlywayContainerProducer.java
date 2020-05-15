package io.quarkus.flyway.runtime;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;

import io.quarkus.datasource.common.runtime.DataSourceUtil;

/**
 * This class is sort of a producer for {@link Flyway}.
 *
 * It isn't a CDI producer in the literal sense, but it is marked as a bean
 * and it's {@code createFlyway} method is called at runtime in order to produce
 * the actual {@code Flyway} objects.
 *
 * CDI scopes and qualifiers are setup at build-time, which is why this class is devoid of
 * any CDI annotations
 *
 */
public class FlywayContainerProducer {

    private final FlywayRuntimeConfig flywayRuntimeConfig;
    private final FlywayBuildTimeConfig flywayBuildConfig;

    public FlywayContainerProducer(FlywayRuntimeConfig flywayRuntimeConfig, FlywayBuildTimeConfig flywayBuildConfig) {
        this.flywayRuntimeConfig = flywayRuntimeConfig;
        this.flywayBuildConfig = flywayBuildConfig;
    }

    public FlywayContainer createFlyway(DataSource dataSource, String dataSourceName) {
        FlywayDataSourceRuntimeConfig matchingRuntimeConfig = DataSourceUtil.isDefault(dataSourceName)
                ? flywayRuntimeConfig.defaultDataSource
                : flywayRuntimeConfig.getConfigForDataSourceName(dataSourceName);
        FlywayDataSourceBuildTimeConfig matchingBuildTimeConfig = DataSourceUtil.isDefault(dataSourceName)
                ? flywayBuildConfig.defaultDataSource
                : flywayBuildConfig.getConfigForDataSourceName(dataSourceName);
        Flyway flyway = new FlywayCreator(matchingRuntimeConfig, matchingBuildTimeConfig).createFlyway(dataSource);
        return new FlywayContainer(flyway, matchingRuntimeConfig.cleanAtStart, matchingRuntimeConfig.migrateAtStart);
    }
}
