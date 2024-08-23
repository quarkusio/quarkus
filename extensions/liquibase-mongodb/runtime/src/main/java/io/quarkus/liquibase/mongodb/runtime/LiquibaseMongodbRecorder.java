package io.quarkus.liquibase.mongodb.runtime;

import java.util.function.Supplier;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.liquibase.mongodb.LiquibaseMongodbFactory;
import io.quarkus.mongodb.runtime.MongodbConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import liquibase.Liquibase;

@Recorder
public class LiquibaseMongodbRecorder {

    private final RuntimeValue<LiquibaseMongodbConfig> config;

    public LiquibaseMongodbRecorder(RuntimeValue<LiquibaseMongodbConfig> config) {
        this.config = config;
    }

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
        if (!config.getValue().enabled) {
            return;
        }
        try {
            InjectableInstance<LiquibaseMongodbFactory> liquibaseFactoryInstance = Arc.container()
                    .select(LiquibaseMongodbFactory.class, Any.Literal.INSTANCE);
            if (liquibaseFactoryInstance.isUnsatisfied()) {
                return;
            }

            for (InstanceHandle<LiquibaseMongodbFactory> liquibaseFactoryHandle : liquibaseFactoryInstance.handles()) {
                try {
                    LiquibaseMongodbFactory liquibaseFactory = liquibaseFactoryHandle.get();

                    if (!liquibaseFactory.getConfiguration().cleanAtStart
                            && !liquibaseFactory.getConfiguration().migrateAtStart) {
                        // Don't initialize if no clean or migration required at start
                        return;
                    }

                    try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                        if (liquibaseFactory.getConfiguration().cleanAtStart) {
                            liquibase.dropAll();
                        }
                        if (liquibaseFactory.getConfiguration().migrateAtStart) {
                            if (liquibaseFactory.getConfiguration().validateOnMigrate) {
                                liquibase.validate();
                            }
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
