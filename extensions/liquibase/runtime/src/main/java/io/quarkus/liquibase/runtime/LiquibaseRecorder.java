package io.quarkus.liquibase.runtime;

import java.util.function.Function;

import javax.sql.DataSource;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.runtime.UnconfiguredDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
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

    public Function<SyntheticCreationalContext<LiquibaseFactory>, LiquibaseFactory> liquibaseFunction(String dataSourceName) {
        DataSource dataSource = DataSources.fromName(dataSourceName);
        if (dataSource instanceof UnconfiguredDataSource) {
            return new Function<SyntheticCreationalContext<LiquibaseFactory>, LiquibaseFactory>() {
                @Override
                public LiquibaseFactory apply(SyntheticCreationalContext<LiquibaseFactory> context) {
                    throw new UnsatisfiedResolutionException("No datasource has been configured");
                }
            };
        }
        return new Function<SyntheticCreationalContext<LiquibaseFactory>, LiquibaseFactory>() {
            @Override
            public LiquibaseFactory apply(SyntheticCreationalContext<LiquibaseFactory> context) {
                LiquibaseFactoryProducer liquibaseProducer = context.getInjectedReference(LiquibaseFactoryProducer.class);
                LiquibaseFactory liquibaseFactory = liquibaseProducer.createLiquibaseFactory(dataSource, dataSourceName);
                return liquibaseFactory;
            }
        };
    }

    public void doStartActions() {
        if (!config.getValue().enabled) {
            return;
        }

        try {
            InjectableInstance<LiquibaseFactory> liquibaseFactoryInstance = Arc.container()
                    .select(LiquibaseFactory.class, Any.Literal.INSTANCE);
            if (liquibaseFactoryInstance.isUnsatisfied()) {
                return;
            }

            for (InstanceHandle<LiquibaseFactory> liquibaseFactoryHandle : liquibaseFactoryInstance.handles()) {
                try {
                    LiquibaseFactory liquibaseFactory = liquibaseFactoryHandle.get();
                    var config = liquibaseFactory.getConfiguration();
                    if (!config.cleanAtStart && !config.migrateAtStart) {
                        continue;
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
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error starting Liquibase", e);
        }
    }
}
