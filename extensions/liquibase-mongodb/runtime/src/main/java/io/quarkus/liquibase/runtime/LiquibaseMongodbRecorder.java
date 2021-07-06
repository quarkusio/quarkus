package io.quarkus.liquibase.runtime;

import java.util.function.Supplier;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.UnsatisfiedResolutionException;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.liquibase.LiquibaseMongodbFactory;
import io.quarkus.mongodb.runtime.MongodbConfig;
import io.quarkus.runtime.annotations.Recorder;
import liquibase.Liquibase;

@Recorder
public class LiquibaseMongodbRecorder {

    public Supplier<LiquibaseMongodbFactory> liquibaseSupplier(LiquibaseMongodbConfig config,
            LiquibaseMongodbBuildTimeConfig buildTimeConfig, MongodbConfig mongodbConfig) {
        return new Supplier<LiquibaseMongodbFactory>() {
            @Override
            public LiquibaseMongodbFactory get() {
                return new LiquibaseMongodbFactory(config, buildTimeConfig, mongodbConfig.defaultMongoClientConfig);
            }
        };
    }

    public void doStartActions() {
        try {
            InjectableInstance<LiquibaseMongodbFactory> liquibaseFactoryInstance = Arc.container()
                    .select(LiquibaseMongodbFactory.class, Any.Literal.INSTANCE);
            if (liquibaseFactoryInstance.isUnsatisfied()) {
                return;
            }

            for (InstanceHandle<LiquibaseMongodbFactory> liquibaseFactoryHandle : liquibaseFactoryInstance.handles()) {
                try {
                    LiquibaseMongodbFactory liquibaseFactory = liquibaseFactoryHandle.get();
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
