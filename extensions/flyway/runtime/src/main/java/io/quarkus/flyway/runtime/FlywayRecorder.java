package io.quarkus.flyway.runtime;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jboss.logging.Logger;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.runtime.UnconfiguredDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.flyway.FlywayDataSource.FlywayDataSourceLiteral;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FlywayRecorder {

    private static final Logger log = Logger.getLogger(FlywayRecorder.class);

    private final RuntimeValue<FlywayRuntimeConfig> config;

    public FlywayRecorder(RuntimeValue<FlywayRuntimeConfig> config) {
        this.config = config;
    }

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

    public Function<SyntheticCreationalContext<FlywayContainer>, FlywayContainer> flywayContainerFunction(String dataSourceName,
            boolean hasMigrations,
            boolean createPossible) {
        return new Function<>() {
            @Override
            public FlywayContainer apply(SyntheticCreationalContext<FlywayContainer> context) {
                DataSource dataSource = context.getInjectedReference(DataSources.class).getDataSource(dataSourceName);
                if (dataSource instanceof UnconfiguredDataSource) {
                    throw new UnsatisfiedResolutionException("No datasource present");
                }

                FlywayContainerProducer flywayProducer = context.getInjectedReference(FlywayContainerProducer.class);
                FlywayContainer flywayContainer = flywayProducer.createFlyway(dataSource, dataSourceName, hasMigrations,
                        createPossible);
                return flywayContainer;
            }
        };
    }

    public Function<SyntheticCreationalContext<Flyway>, Flyway> flywayFunction(String dataSourceName) {
        return new Function<>() {
            @Override
            public Flyway apply(SyntheticCreationalContext<Flyway> context) {
                Annotation flywayContainerQualifier;
                if (DataSourceUtil.isDefault(dataSourceName)) {
                    flywayContainerQualifier = Default.Literal.INSTANCE;
                } else {
                    flywayContainerQualifier = FlywayDataSourceLiteral.of(dataSourceName);
                }

                FlywayContainer flywayContainer = context.getInjectedReference(FlywayContainer.class, flywayContainerQualifier);
                return flywayContainer.getFlyway();
            }
        };
    }

    public void doStartActions() {
        if (!config.getValue().enabled) {
            return;
        }

        for (InstanceHandle<FlywayContainer> flywayContainerHandle : Arc.container().listAll(FlywayContainer.class)) {
            FlywayContainer flywayContainer = flywayContainerHandle.get();

            if (flywayContainer.isCleanAtStart()) {
                flywayContainer.getFlyway().clean();
            }
            if (flywayContainer.isValidateAtStart()) {
                flywayContainer.getFlyway().validate();
            }
            if (flywayContainer.isRepairAtStart()) {
                flywayContainer.getFlyway().repair();
            }
            if (flywayContainer.isMigrateAtStart()) {
                flywayContainer.getFlyway().migrate();
            }
        }
    }
}
