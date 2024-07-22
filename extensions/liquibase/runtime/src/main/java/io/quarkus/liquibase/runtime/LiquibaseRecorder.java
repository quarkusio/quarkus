package io.quarkus.liquibase.runtime;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.sql.DataSource;

import io.quarkus.agroal.runtime.AgroalDataSourceUtil;
import io.quarkus.agroal.runtime.UnconfiguredDataSource;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.runtime.ResettableSystemProperties;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import liquibase.Liquibase;
import liquibase.lockservice.LockServiceFactory;

@Recorder
public class LiquibaseRecorder {

    private final RuntimeValue<LiquibaseRuntimeConfig> config;

    public LiquibaseRecorder(RuntimeValue<LiquibaseRuntimeConfig> config) {
        this.config = config;
    }

    public Supplier<ActiveResult> liquibaseActiveSupplier(String dataSourceName) {
        return new Supplier<ActiveResult>() {
            @Override
            public ActiveResult get() {
                // Flyway beans are inactive when the datasource itself is inactive.
                var dataSourceBean = AgroalDataSourceUtil.dataSourceInstance(dataSourceName).getHandle().getBean();
                var dataSourceActive = dataSourceBean.isActive();
                if (!dataSourceActive.result()) {
                    return ActiveResult.inactive(
                            String.format(Locale.ROOT,
                                    "Liquibase for datasource '%s' was deactivated automatically because this datasource was deactivated.",
                                    dataSourceName),
                            dataSourceActive);
                }

                // Liquibase beans are inactive when the datasource is unconfigured.
                var dataSource = ClientProxy.unwrap(AgroalDataSourceUtil.dataSourceInstance(dataSourceName).get());
                if (dataSource instanceof UnconfiguredDataSource) {
                    var cause = DataSourceUtil.dataSourceNotConfigured(dataSourceName);
                    return ActiveResult.inactive(String.format(Locale.ROOT,
                            "Liquibase for datasource '%s' was deactivated automatically because this datasource was not configured. "
                                    + cause.getMessage(),
                            dataSourceName));
                }

                // Note: When quarkus.liquibase.enabled is set to false, Liquibase beans are still available.
                //       The property only controls automatic execution on startup.
                // TODO should we change quarkus.liquibase.enabled (see ^) to align on other extensions?
                //   We'd have something like quarkus.liquibase.startup.enabled controlling startup behavior,
                //   and *if necessary* quarkus.liquibase.active controlling bean availability
                //   (though IMO controlling that at the datasource level would be enough).
                return ActiveResult.active();
            }
        };
    }

    public Function<SyntheticCreationalContext<LiquibaseFactory>, LiquibaseFactory> liquibaseFunction(String dataSourceName) {
        return new Function<SyntheticCreationalContext<LiquibaseFactory>, LiquibaseFactory>() {
            @Override
            public LiquibaseFactory apply(SyntheticCreationalContext<LiquibaseFactory> context) {
                DataSource dataSource = context.getInjectedReference(DataSource.class,
                        AgroalDataSourceUtil.qualifier(dataSourceName));
                LiquibaseFactoryProducer liquibaseProducer = context.getInjectedReference(LiquibaseFactoryProducer.class);
                return liquibaseProducer.createLiquibaseFactory(dataSource, dataSourceName);
            }
        };
    }

    public void doStartActions(String dataSourceName) {
        if (!config.getValue().enabled) {
            return;
        }

        InjectableInstance<LiquibaseFactory> liquibaseFactoryInstance = Arc.container().select(LiquibaseFactory.class,
                LiquibaseFactoryUtil.getLiquibaseFactoryQualifier(dataSourceName));
        if (!liquibaseFactoryInstance.isResolvable()
                || !liquibaseFactoryInstance.getHandle().getBean().isActive().result()) {
            return;
        }
        try {
            LiquibaseFactory liquibaseFactory = liquibaseFactoryInstance.get();
            var config = liquibaseFactory.getConfiguration();
            if (!config.cleanAtStart && !config.migrateAtStart) {
                return;
            }
            try (Liquibase liquibase = liquibaseFactory.createLiquibase();
                    ResettableSystemProperties resettableSystemProperties = liquibaseFactory
                            .createResettableSystemProperties()) {
                if (config.cleanAtStart) {
                    liquibase.dropAll();
                }
                if (config.migrateAtStart) {
                    var lockService = LockServiceFactory.getInstance()
                            .getLockService(liquibase.getDatabase());
                    lockService.waitForLock();
                    try {
                        if (config.validateOnMigrate) {
                            liquibase.validate();
                        }
                        liquibase.update(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
                    } finally {
                        lockService.releaseLock();
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error starting Liquibase", e);
        }
    }

}
