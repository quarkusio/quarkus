package io.quarkus.mongo.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "mongodb", phase = ConfigPhase.RUN_TIME)
public class MongoClientConfig {

    // TODO Extend this.

    /**
     * Configure the connection string.
     */
    @ConfigItem
    public String connectionString;

}
