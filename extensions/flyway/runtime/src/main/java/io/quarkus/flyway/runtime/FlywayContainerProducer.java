package io.quarkus.flyway.runtime;

import java.util.Collection;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;

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

    public FlywayContainer createFlyway(DataSource dataSource, String dataSourceName, boolean hasMigrations,
            boolean createPossible) {
        FlywayDataSourceRuntimeConfig matchingRuntimeConfig = flywayRuntimeConfig.getConfigForDataSourceName(dataSourceName);
        FlywayDataSourceBuildTimeConfig matchingBuildTimeConfig = flywayBuildConfig.getConfigForDataSourceName(dataSourceName);
        final Collection<Callback> callbacks = QuarkusPathLocationScanner.callbacksForDataSource(dataSourceName);
        final Flyway flyway = new FlywayCreator(matchingRuntimeConfig, matchingBuildTimeConfig).withCallbacks(callbacks)
                .createFlyway(dataSource);
        return new FlywayContainer(flyway, matchingRuntimeConfig.cleanAtStart, matchingRuntimeConfig.migrateAtStart,
                dataSourceName, hasMigrations, createPossible);
    }
}
