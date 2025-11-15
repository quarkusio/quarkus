package io.quarkus.liquibase.mongodb.runtime;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.liquibase.mongodb.LiquibaseMongodbFactory;
import io.quarkus.mongodb.runtime.MongoClientConfig;
import io.quarkus.mongodb.runtime.MongoConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import liquibase.Liquibase;

@Recorder
public class LiquibaseMongodbRecorder {
    private final LiquibaseMongodbBuildTimeConfig buildTimeConfig;
    private final RuntimeValue<LiquibaseMongodbConfig> runtimeConfig;
    private final RuntimeValue<MongoConfig> mongodbRuntimeConfig;

    public LiquibaseMongodbRecorder(
            final LiquibaseMongodbBuildTimeConfig buildTimeConfig,
            final RuntimeValue<LiquibaseMongodbConfig> runtimeConfig,
            final RuntimeValue<MongoConfig> mongodbRuntimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
        this.runtimeConfig = runtimeConfig;
        this.mongodbRuntimeConfig = mongodbRuntimeConfig;
    }

    public Supplier<LiquibaseMongodbFactory> liquibaseSupplier(String clientName) {
        return new Supplier<LiquibaseMongodbFactory>() {
            private <T> T getRequiredConfig(Map<String, T> map, String errorMessage) {
                T value = map.get(clientName);
                if (value == null) {
                    throw new IllegalArgumentException(errorMessage.formatted(clientName));
                }
                return value;
            }

            @Override
            public LiquibaseMongodbFactory get() {
                LiquibaseMongodbBuildTimeClientConfig buildTimeClientConfig = getRequiredConfig(
                        buildTimeConfig.clientConfigs(),
                        "Liquibase Mongo config (changeLog) named '%s' not found");

                LiquibaseMongodbClientConfig liquibaseMongodbClientConfig = getRequiredConfig(
                        runtimeConfig.getValue().clientConfigs(),
                        "Liquibase Mongo client config named '%s' not found");

                MongoClientConfig mongoClientConfig;
                String clientNameSelected;
                if (liquibaseMongodbClientConfig.mongoClientName().isPresent()) {
                    // keep compatibility with the legacy configuration which makes possible set the mongo-client-name
                    String forceMongoClientName = liquibaseMongodbClientConfig.mongoClientName().get();
                    mongoClientConfig = mongodbRuntimeConfig.getValue().clients().get(forceMongoClientName);
                    if (mongoClientConfig == null) {
                        throw new IllegalArgumentException(
                                "Mongo client named '%s' not found".formatted(forceMongoClientName));
                    }
                    clientNameSelected = forceMongoClientName;
                } else if (MongoConfig.isDefaultClient(clientName)) {
                    mongoClientConfig = mongodbRuntimeConfig.getValue().clients().get(clientName);
                    clientNameSelected = clientName;
                } else {
                    mongoClientConfig = getRequiredConfig(mongodbRuntimeConfig.getValue().clients(),
                            "Mongo client named '%s' not found");
                    clientNameSelected = clientName;
                }
                return new LiquibaseMongodbFactory(
                        liquibaseMongodbClientConfig,
                        buildTimeClientConfig,
                        mongoClientConfig,
                        clientNameSelected);
            }
        };
    }

    private Annotation getLiquibaseMongodbQualifier(String clientName) {
        if (MongoConfig.isDefaultClient(clientName)) {
            return Default.Literal.INSTANCE;
        } else {
            return LiquibaseMongodbClient.LiquibaseMongodbClientLiteral.of(clientName);
        }
    }

    public void doStartActions(String clientName) {
        if (!runtimeConfig.getValue().enabled()) {
            return;
        }

        try {
            InjectableInstance<LiquibaseMongodbFactory> liquibaseFactoryInstance = Arc.container()
                    .select(LiquibaseMongodbFactory.class, getLiquibaseMongodbQualifier(clientName));
            if (liquibaseFactoryInstance.isUnsatisfied()) {
                return;
            }

            for (InstanceHandle<LiquibaseMongodbFactory> liquibaseFactoryHandle : liquibaseFactoryInstance.handles()) {
                try {
                    LiquibaseMongodbFactory liquibaseFactory = liquibaseFactoryHandle.get();

                    if (!liquibaseFactory.getConfiguration().cleanAtStart()
                            && !liquibaseFactory.getConfiguration().migrateAtStart()) {
                        // Don't initialize if no clean or migration required at start
                        return;
                    }

                    try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
                        if (liquibaseFactory.getConfiguration().cleanAtStart()) {
                            liquibase.dropAll();
                        }
                        if (liquibaseFactory.getConfiguration().migrateAtStart()) {
                            if (liquibaseFactory.getConfiguration().validateOnMigrate()) {
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
