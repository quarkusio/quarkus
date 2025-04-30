package io.quarkus.flyway.runtime;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

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

import io.quarkus.agroal.runtime.AgroalDataSourceUtil;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.SyntheticCreationalContext;
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

    public Supplier<ActiveResult> flywayCheckActiveSupplier(String dataSourceName) {
        return new Supplier<ActiveResult>() {
            @Override
            public ActiveResult get() {
                // Flyway beans are inactive when the datasource itself is inactive.
                var dataSourceBean = AgroalDataSourceUtil.dataSourceInstance(dataSourceName).getHandle().getBean();
                var dataSourceActive = dataSourceBean.checkActive();
                if (!dataSourceActive.value()) {
                    return ActiveResult.inactive(
                            String.format(Locale.ROOT,
                                    "Flyway for datasource '%s' was deactivated automatically because this datasource was deactivated.",
                                    dataSourceName),
                            dataSourceActive);
                }

                // Note: When quarkus.flyway.active is set to false, Flyway beans are still available.
                //       The property only controls automatic execution on startup.
                // TODO should we change quarkus.flyway.active (see ^) to align on other extensions?
                //   See https://github.com/quarkusio/quarkus/issues/42244.
                //   We'd have something like quarkus.liquibase.startup.enabled controlling startup behavior,
                //   and *if necessary* quarkus.liquibase.active controlling bean availability
                //   (though IMO controlling that at the datasource level would be enough).
                return ActiveResult.active();
            }
        };
    }

    public Function<SyntheticCreationalContext<FlywayContainer>, FlywayContainer> flywayContainerFunction(String dataSourceName,
            boolean hasMigrations,
            boolean createPossible) {
        return new Function<>() {
            @Override
            public FlywayContainer apply(SyntheticCreationalContext<FlywayContainer> context) {
                DataSource dataSource = context.getInjectedReference(DataSource.class,
                        AgroalDataSourceUtil.qualifier(dataSourceName));
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
                .datasources().get(dataSourceName);

        if (flywayDataSourceRuntimeConfig.active().isPresent()
                && !flywayDataSourceRuntimeConfig.active().get()) {
            return;
        }

        InjectableInstance<FlywayContainer> flywayContainerInstance = Arc.container().select(FlywayContainer.class,
                FlywayContainerUtil.getFlywayContainerQualifier(dataSourceName));
        if (!flywayContainerInstance.isResolvable()
                || !flywayContainerInstance.getHandle().getBean().isActive()) {
            return;
        }

        FlywayContainer flywayContainer = flywayContainerInstance.get();

        if (flywayContainer.isCleanAtStart()) {
            flywayContainer.getFlyway().clean();
        }
        if (flywayContainer.isValidateAtStart()) {
            if (flywayContainer.isCleanOnValidationError()) {
                var result = flywayContainer.getFlyway().validateWithResult();

                if (!result.validationSuccessful) {
                    flywayContainer.getFlyway().clean();
                }
            } else {
                flywayContainer.getFlyway().validate();
            }
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
