package io.quarkus.flyway.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.sql.DataSource;

import jakarta.enterprise.inject.UnsatisfiedResolutionException;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jboss.logging.Logger;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.runtime.UnconfiguredDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FlywayRecorder {

    private static final Logger log = Logger.getLogger(FlywayRecorder.class);

    static final List<FlywayContainer> FLYWAY_CONTAINERS = new ArrayList<>(2);

    public void setApplicationMigrationFiles(Collection<String> migrationFiles) {
        log.debugv("Setting the following application migration files: {0}", migrationFiles);
        QuarkusPathLocationScanner.setApplicationMigrationFiles(migrationFiles);
    }

    public void setApplicationMigrationClasses(Collection<Class<? extends JavaMigration>> migrationClasses) {
        log.debugv("Setting the following application migration classes: {0}", migrationClasses);
        QuarkusPathLocationScanner.setApplicationMigrationClasses(migrationClasses);
    }

    public void setApplicationCallbackClasses(Map<String, Collection<Callback>> callbackClasses) {
        log.debugv("Setting application callbacks: {0} total", callbackClasses.values().size());
        QuarkusPathLocationScanner.setApplicationCallbackClasses(callbackClasses);
    }

    public void resetFlywayContainers() {
        FLYWAY_CONTAINERS.clear();
    }

    public Supplier<FlywayContainer> flywayContainerSupplier(String dataSourceName, boolean hasMigrations,
            boolean createPossible) {
        DataSource dataSource = DataSources.fromName(dataSourceName);
        if (dataSource instanceof UnconfiguredDataSource) {
            return new Supplier<FlywayContainer>() {
                @Override
                public FlywayContainer get() {
                    throw new UnsatisfiedResolutionException("No datasource present");
                }
            };
        }

        FlywayContainerProducer flywayContainerProducer = Arc.container()
                .instance(FlywayContainerProducer.class).get();
        FlywayContainer flywayContainer = flywayContainerProducer.createFlyway(dataSource, dataSourceName,
                hasMigrations, createPossible);
        FLYWAY_CONTAINERS.add(flywayContainer);

        return new Supplier<FlywayContainer>() {
            @Override
            public FlywayContainer get() {
                return flywayContainer;
            }
        };
    }

    public Supplier<Flyway> flywaySupplier(String dataSourceName, boolean hasMigrations, boolean createPossible) {
        DataSource dataSource = DataSources.fromName(dataSourceName);
        if (dataSource instanceof UnconfiguredDataSource) {
            return new Supplier<Flyway>() {
                @Override
                public Flyway get() {
                    throw new UnsatisfiedResolutionException("No datasource present");
                }
            };
        }
        FlywayContainer flywayContainer = FLYWAY_CONTAINERS.stream().filter(c -> dataSourceName.equals(c.getDataSourceName()))
                .findFirst()
                .orElseGet(() -> {
                    return flywayContainerSupplier(dataSourceName, hasMigrations, createPossible).get();
                });

        return new Supplier<Flyway>() {
            @Override
            public Flyway get() {
                return flywayContainer.getFlyway();
            }
        };
    }
}
