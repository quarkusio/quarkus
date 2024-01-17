package io.quarkus.flyway.runtime;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.FlywayExecutor;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.output.BaselineResult;
import org.flywaydb.core.internal.callback.CallbackExecutor;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.database.base.Schema;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.core.internal.resolver.CompositeMigrationResolver;
import org.flywaydb.core.internal.schemahistory.SchemaHistory;
import org.jboss.logging.Logger;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.runtime.UnconfiguredDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

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
                DataSource dataSource;
                try {
                    dataSource = context.getInjectedReference(DataSources.class).getDataSource(dataSourceName);
                    if (dataSource instanceof UnconfiguredDataSource) {
                        throw DataSourceUtil.dataSourceNotConfigured(dataSourceName);
                    }
                } catch (ConfigurationException e) {
                    // TODO do we really want to enable retrieval of a FlywayContainer for an unconfigured/inactive datasource?
                    //   Assigning ApplicationScoped to the FlywayContainer
                    //   and throwing UnsatisfiedResolutionException on bean creation (first access)
                    //   would probably make more sense.
                    return new UnconfiguredDataSourceFlywayContainer(dataSourceName, String.format(Locale.ROOT,
                            "Unable to find datasource '%s' for Flyway: %s",
                            dataSourceName, e.getMessage()), e);
                }

                FlywayContainerProducer flywayProducer = context.getInjectedReference(FlywayContainerProducer.class);
                return flywayProducer.createFlyway(dataSource, dataSourceName, hasMigrations, createPossible);
            }
        };
    }

    public Function<SyntheticCreationalContext<Flyway>, Flyway> flywayFunction(String dataSourceName) {
        return new Function<>() {
            @Override
            public Flyway apply(SyntheticCreationalContext<Flyway> context) {
                FlywayContainer flywayContainer = context.getInjectedReference(FlywayContainer.class,
                        FlywayContainerUtil.getFlywayContainerQualifier(dataSourceName));
                return flywayContainer.getFlyway();
            }
        };
    }

    public void doStartActions(String dataSourceName) {
        FlywayDataSourceRuntimeConfig flywayDataSourceRuntimeConfig = config.getValue()
                .getConfigForDataSourceName(dataSourceName);

        if (!flywayDataSourceRuntimeConfig.active
                // If not specified explicitly, Flyway is active when the datasource itself is active.
                .orElseGet(() -> Arc.container().instance(DataSources.class).get().getActiveDataSourceNames()
                        .contains(dataSourceName))) {
            return;
        }

        InstanceHandle<FlywayContainer> flywayContainerInstanceHandle = Arc.container().instance(FlywayContainer.class,
                FlywayContainerUtil.getFlywayContainerQualifier(dataSourceName));

        if (!flywayContainerInstanceHandle.isAvailable()) {
            return;
        }

        FlywayContainer flywayContainer = flywayContainerInstanceHandle.get();

        if (flywayContainer instanceof UnconfiguredDataSourceFlywayContainer) {
            return;
        }

        if (flywayContainer.isCleanAtStart()) {
            flywayContainer.getFlyway().clean();
        }
        if (flywayContainer.isValidateAtStart()) {
            flywayContainer.getFlyway().validate();
        }
        if (flywayContainer.isBaselineAtStart()) {
            new FlywayExecutor(flywayContainer.getFlyway().getConfiguration())
                    .execute(new BaselineCommand(flywayContainer.getFlyway()), true, null);
        }
        if (flywayContainer.isRepairAtStart()) {
            flywayContainer.getFlyway().repair();
        }
        if (flywayContainer.isMigrateAtStart()) {
            flywayContainer.getFlyway().migrate();
        }
    }

    static class BaselineCommand implements FlywayExecutor.Command<BaselineResult> {
        BaselineCommand(Flyway flyway) {
            this.flyway = flyway;
        }

        final Flyway flyway;

        @Override
        public BaselineResult execute(CompositeMigrationResolver cmr, SchemaHistory schemaHistory, Database d,
                Schema defaultSchema, Schema[] s, CallbackExecutor ce, StatementInterceptor si) {
            if (!schemaHistory.exists()) {
                return flyway.baseline();
            }
            return null;
        }
    }
}
