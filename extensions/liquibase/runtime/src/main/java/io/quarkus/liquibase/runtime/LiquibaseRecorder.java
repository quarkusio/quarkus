package io.quarkus.liquibase.runtime;

import java.util.function.Supplier;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.sql.DataSource;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.runtime.UnconfiguredDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.runtime.annotations.Recorder;
import liquibase.Liquibase;

@Recorder
public class LiquibaseRecorder {

    public Supplier<LiquibaseFactory> liquibaseSupplier(String dataSourceName) {
        DataSource dataSource = DataSources.fromName(dataSourceName);
        if (dataSource instanceof UnconfiguredDataSource) {
            return new Supplier<LiquibaseFactory>() {
                @Override
                public LiquibaseFactory get() {
                    throw new UnsatisfiedResolutionException("No datasource has been configured");
                }
            };
        }
        LiquibaseFactoryProducer liquibaseProducer = Arc.container().instance(LiquibaseFactoryProducer.class).get();
        LiquibaseFactory liquibaseFactory = liquibaseProducer.createLiquibaseFactory(dataSource, dataSourceName);
        return new Supplier<LiquibaseFactory>() {
            @Override
            public LiquibaseFactory get() {
                return liquibaseFactory;
            }
        };
    }

    public void doStartActions() {
        try {
            InjectableInstance<LiquibaseFactory> liquibaseFactoryInstance = Arc.container()
                    .select(LiquibaseFactory.class, Any.Literal.INSTANCE);
            if (liquibaseFactoryInstance.isUnsatisfied()) {
                return;
            }

            for (InstanceHandle<LiquibaseFactory> liquibaseFactoryHandle : liquibaseFactoryInstance.handles()) {
                try {
                    LiquibaseFactory liquibaseFactory = liquibaseFactoryHandle.get();
                    if (liquibaseFactory.getConfiguration().cleanAtStart) {
                        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                            liquibase.dropAll();
                        }
                    }
                    if (liquibaseFactory.getConfiguration().migrateAtStart) {
                        if (liquibaseFactory.getConfiguration().validateOnMigrate) {
                            try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                                liquibase.validate();
                            }
                        }
                        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                            liquibase.update(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
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
