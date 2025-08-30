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
import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
import io.quarkus.mongodb.runtime.MongoClientConfig;
import io.quarkus.mongodb.runtime.MongodbConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import liquibase.Liquibase;

@Recorder
public class LiquibaseMongodbRecorder {
    private final LiquibaseMongodbBuildTimeConfig buildTimeConfig;
    private final RuntimeValue<LiquibaseMongodbConfig> runtimeConfig;
    private final RuntimeValue<MongodbConfig> mongodbRuntimeConfig;

    public LiquibaseMongodbRecorder(
            final LiquibaseMongodbBuildTimeConfig buildTimeConfig,
            final RuntimeValue<LiquibaseMongodbConfig> runtimeConfig,
            final RuntimeValue<MongodbConfig> mongodbRuntimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
        this.runtimeConfig = runtimeConfig;
        this.mongodbRuntimeConfig = mongodbRuntimeConfig;
    }

    public Supplier<LiquibaseMongodbFactory> liquibaseSupplier(String dataSourceName) {
        return new Supplier<LiquibaseMongodbFactory>() {
            private <T> T getRequiredConfig(Map<String, T> map, String errorMessage) {
                T value = map.get(dataSourceName);
                if (value == null) {
                    throw new IllegalArgumentException(errorMessage.formatted(dataSourceName));
                }
                return value;
            }

            @Override
            public LiquibaseMongodbFactory get() {
                LiquibaseMongodbBuildTimeDataSourceConfig buildTimeClientConfig = getRequiredConfig(
                        buildTimeConfig.dataSourceConfigs(),
                        "Liquibase Mongo config (changeLog) named '%s' not found");

                LiquibaseMongodbDataSourceConfig liquibaseMongodbDataSourceConfig = getRequiredConfig(
                        runtimeConfig.getValue().dataSourceConfigs(),
                        "Liquibase Mongo datasource config named '%s' not found");

                MongoClientConfig mongoClientConfig;
                String dataSourceNameSelected;
                if (liquibaseMongodbDataSourceConfig.mongoClientName().isPresent()) {
                    // keep compatibility with the legacy configuration which makes possible set the mongo-client-name
                    String forceMongoClientName = liquibaseMongodbDataSourceConfig.mongoClientName().get();
                    mongoClientConfig = mongodbRuntimeConfig.getValue().mongoClientConfigs().get(forceMongoClientName);
                    if (mongoClientConfig == null) {
                        throw new IllegalArgumentException(
                                "Mongo client named '%s' not found".formatted(forceMongoClientName));
                    }
                    dataSourceNameSelected = forceMongoClientName;
                } else if (MongoClientBeanUtil.isDefault(dataSourceName)) {
                    mongoClientConfig = mongodbRuntimeConfig.getValue().defaultMongoClientConfig();
                    dataSourceNameSelected = dataSourceName;
                } else {
                    mongoClientConfig = getRequiredConfig(
                            mongodbRuntimeConfig.getValue().mongoClientConfigs(),
                            "Mongo client named '%s' not found");
                    dataSourceNameSelected = dataSourceName;
                }
                return new LiquibaseMongodbFactory(
                        liquibaseMongodbDataSourceConfig,
                        buildTimeClientConfig,
                        mongoClientConfig,
                        dataSourceNameSelected);
            }
        };
    }

    private Annotation getLiquibaseMongodbQualifier(String dataSourceName) {
        if (MongoClientBeanUtil.isDefault(dataSourceName)) {
            return Default.Literal.INSTANCE;
        } else {
            return LiquibaseMongodbDataSource.LiquibaseMongodbDataSourceLiteral.of(dataSourceName);
        }
    }

    public void doStartActions(String dataSourceName) {
        if (!runtimeConfig.getValue().enabled()) {
            return;
        }

        try {
            InjectableInstance<LiquibaseMongodbFactory> liquibaseFactoryInstance = Arc.container()
                    .select(LiquibaseMongodbFactory.class, getLiquibaseMongodbQualifier(dataSourceName));
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
