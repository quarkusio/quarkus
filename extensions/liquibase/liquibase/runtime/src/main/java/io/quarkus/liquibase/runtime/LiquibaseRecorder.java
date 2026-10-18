package io.quarkus.liquibase.runtime;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.sql.DataSource;

import io.quarkus.agroal.runtime.AgroalDataSourceUtil;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InactiveBeanException;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.liquibase.LiquibaseFactory;
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

    public Supplier<ActiveResult> liquibaseCheckActiveSupplier(String dataSourceName) {
        return new Supplier<ActiveResult>() {
            @Override
            public ActiveResult get() {
                // Flyway beans are inactive when the datasource itself is inactive.
                var dataSourceBean = AgroalDataSourceUtil.dataSourceInstance(dataSourceName).getHandle().getBean();
                var dataSourceActive = dataSourceBean.checkActive();
                if (!dataSourceActive.value()) {
                    return ActiveResult.inactive(
                            String.format(Locale.ROOT,
                                    "Liquibase for datasource '%s' was deactivated automatically because this datasource was deactivated.",
                                    dataSourceName),
                            dataSourceActive);
                }

                // Note: When quarkus.liquibase.enabled is set to false, Liquibase beans are still available.
                //       The property only controls automatic execution on startup.
                // TODO should we change quarkus.liquibase.enabled (see ^) to align on other extensions?
                //   See https://github.com/quarkusio/quarkus/issues/42244.
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
        if (!config.getValue().enabled()) {
            return;
        }

        var dataSourceConfig = config.getValue().datasources().get(dataSourceName);
        if (!dataSourceConfig.cleanAtStart() && !dataSourceConfig.migrateAtStart()) {
            return;
        }

        InjectableInstance<LiquibaseFactory> liquibaseFactoryInstance = Arc.container().select(LiquibaseFactory.class,
                LiquibaseFactoryUtil.getLiquibaseFactoryQualifier(dataSourceName));
        if (!liquibaseFactoryInstance.isResolvable()
                || !liquibaseFactoryInstance.getHandle().getBean().isActive()) {
            return;
        }

        LiquibaseFactory liquibaseFactory = liquibaseFactoryInstance.get();
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            if (dataSourceConfig.cleanAtStart()) {
                liquibase.dropAll();
            }
            if (dataSourceConfig.migrateAtStart()) {
                var lockService = LockServiceFactory.getInstance()
                        .getLockService(liquibase.getDatabase());
                lockService.waitForLock();
                try {
                    if (dataSourceConfig.validateOnMigrate()) {
                        liquibase.validate();
                    }
                    liquibase.update(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
                } finally {
                    lockService.releaseLock();
                }
            }
        } catch (InactiveBeanException e) {
            // These exceptions should be self-explanatory
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Error starting Liquibase", e);
        }
    }

}
