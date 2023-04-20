package io.quarkus.liquibase.mongodb.runtime;

import java.util.function.Supplier;

import io.quarkus.liquibase.mongodb.LiquibaseMongodbFactory;
import io.quarkus.mongodb.runtime.MongodbConfig;
import io.quarkus.runtime.annotations.Recorder;

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
}
