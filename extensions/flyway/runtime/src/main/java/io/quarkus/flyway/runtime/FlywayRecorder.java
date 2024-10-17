package io.quarkus.flyway.runtime;

import java.util.Collection;
import java.util.List;
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
import io.quarkus.flyway.FlywayTenantSupport;
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
            String name, boolean multiTenancyEnabled,
            Class<? extends ContainerProducer> containerProducerClass, boolean hasMigrations,
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
                    return new UnconfiguredDataSourceFlywayContainer(dataSourceName, name, String.format(Locale.ROOT,
                            "Unable to find datasource '%s' for Flyway: %s",
                            dataSourceName, e.getMessage()), e);
                }

                ContainerProducer containerProducer = context.getInjectedReference(containerProducerClass);

                FlywayDataSourceRuntimeConfig matchingRuntimeConfig = containerProducer.getRuntimeConfig(name);
                FlywayDataSourceBuildTimeConfig matchingBuildTimeConfig = containerProducer.getBuildTimeConfig(name);
                final Collection<Callback> callbacks = QuarkusPathLocationScanner.callbacksForDataSource(name);
                final FlywayCreator flyway = new FlywayCreator(matchingRuntimeConfig, matchingBuildTimeConfig,
                        containerProducer.matchingConfigCustomizers(name))
                        .withCallbacks(callbacks);
                return new FlywayContainer(flyway, dataSource, matchingRuntimeConfig.baselineAtStart,
                        matchingRuntimeConfig.cleanAtStart,
                        matchingRuntimeConfig.migrateAtStart,
                        matchingRuntimeConfig.repairAtStart, matchingRuntimeConfig.validateAtStart,
                        dataSourceName, name, multiTenancyEnabled, hasMigrations,
                        createPossible);
            }
        };
    }

    public Function<SyntheticCreationalContext<Flyway>, Flyway> flywayFunction(String name, boolean multiTenancyEnabled,
            Class<? extends ContainerProducer> containerProducerClass) {
        return new Function<>() {
            @Override
            public Flyway apply(SyntheticCreationalContext<Flyway> context) {
                ContainerProducer containerProducer = context.getInjectedReference(containerProducerClass);
                FlywayContainer flywayContainer = context.getInjectedReference(FlywayContainer.class,
                        containerProducer.getFlywayContainerQualifier(name));
                if (multiTenancyEnabled) {
                    return flywayContainer.getFlyway(containerProducer.getTenantId(context));
                }
                return flywayContainer.getFlyway();
            }
        };
    }

    public void doStartActions(String name, Class<? extends ContainerProducer> containerProducerClass) {

        ContainerProducer containerProducer = Arc.container().instance(containerProducerClass).get();

        FlywayDataSourceRuntimeConfig flywayDataSourceRuntimeConfig = containerProducer.getRuntimeConfig(name);

        InstanceHandle<FlywayContainer> flywayContainerInstanceHandle = Arc.container().instance(FlywayContainer.class,
                containerProducer.getFlywayContainerQualifier(name));

        if (!flywayContainerInstanceHandle.isAvailable()) {
            return;
        }

        FlywayContainer flywayContainer = flywayContainerInstanceHandle.get();

        if (!flywayDataSourceRuntimeConfig.active
                // If not specified explicitly, Flyway is active when the datasource itself is active.
                .orElseGet(() -> Arc.container().instance(DataSources.class).get().getActiveDataSourceNames()
                        .contains(flywayContainer.getDataSourceName()))) {
            return;
        }

        if (flywayContainer instanceof UnconfiguredDataSourceFlywayContainer) {
            return;
        }

        if (flywayContainer.isMultiTenancyEnabled()) {
            InstanceHandle<FlywayTenantSupport> tenantSupportInstance = Arc.container().instance(FlywayTenantSupport.class,
                    containerProducer.getFlywayContainerQualifier(name));
            if (!tenantSupportInstance.isAvailable()) {
                return;
            }
            FlywayTenantSupport flywayTenantSupport = tenantSupportInstance.get();
            List<String> tenantsToInitialize = flywayTenantSupport.getTenantsToInitialize();
            if (tenantsToInitialize == null || tenantsToInitialize.isEmpty()) {
                return;
            }
            tenantsToInitialize.forEach(tenantId -> initialize(flywayContainer, flywayContainer.getFlyway(tenantId)));
        } else {
            initialize(flywayContainer, flywayContainer.getFlyway());
        }
    }

    private static void initialize(FlywayContainer flywayContainer, Flyway flyway) {
        if (flywayContainer.isCleanAtStart()) {
            flyway.clean();
        }
        if (flywayContainer.isValidateAtStart()) {
            flyway.validate();
        }
        if (flywayContainer.isBaselineAtStart()) {
            new FlywayExecutor(flyway.getConfiguration())
                    .execute(new BaselineCommand(flyway), true, null);
        }
        if (flywayContainer.isRepairAtStart()) {
            flyway.repair();
        }
        if (flywayContainer.isMigrateAtStart()) {
            flyway.migrate();
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
