package io.quarkus.liquibase.runtime;

import java.util.Locale;
import java.util.function.Function;

import javax.sql.DataSource;

import jakarta.enterprise.inject.UnsatisfiedResolutionException;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.runtime.UnconfiguredDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
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

    public Function<SyntheticCreationalContext<LiquibaseFactory>, LiquibaseFactory> liquibaseFunction(String dataSourceName) {
        return new Function<SyntheticCreationalContext<LiquibaseFactory>, LiquibaseFactory>() {
            @Override
            public LiquibaseFactory apply(SyntheticCreationalContext<LiquibaseFactory> context) {
                DataSource dataSource;
                try {
                    dataSource = context.getInjectedReference(DataSources.class).getDataSource(dataSourceName);
                    if (dataSource instanceof UnconfiguredDataSource) {
                        throw DataSourceUtil.dataSourceNotConfigured(dataSourceName);
                    }
                } catch (RuntimeException e) {
                    throw new UnsatisfiedResolutionException(String.format(Locale.ROOT,
                            "Unable to find datasource '%s' for Liquibase: %s",
                            dataSourceName, e.getMessage()), e);
                }

                LiquibaseFactoryProducer liquibaseProducer = context.getInjectedReference(LiquibaseFactoryProducer.class);
                return liquibaseProducer.createLiquibaseFactory(dataSource, dataSourceName);
            }
        };
    }

    public void doStartActions(String dataSourceName) {
        if (!config.getValue().enabled) {
            return;
        }
        // Liquibase is active when the datasource itself is active.
        if (!Arc.container().instance(DataSources.class).get().getActiveDataSourceNames().contains(dataSourceName)) {
            return;
        }

        InstanceHandle<LiquibaseFactory> liquibaseFactoryHandle = LiquibaseFactoryUtil.getLiquibaseFactory(dataSourceName);
        try {
            LiquibaseFactory liquibaseFactory = liquibaseFactoryHandle.get();
            var config = liquibaseFactory.getConfiguration();
            if (!config.cleanAtStart && !config.migrateAtStart) {
                return;
            }
            try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
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
        } catch (UnsatisfiedResolutionException e) {
            //ignore, the DS is not configured
        } catch (Exception e) {
            throw new IllegalStateException("Error starting Liquibase", e);
        }
    }

}
