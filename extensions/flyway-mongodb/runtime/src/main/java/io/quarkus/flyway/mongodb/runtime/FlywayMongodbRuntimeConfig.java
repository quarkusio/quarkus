package io.quarkus.flyway.mongodb.runtime;

import java.util.Map;

import io.quarkus.mongodb.runtime.MongoConfig;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

/**
 * Runtime configuration for the Flyway-MongoDB extension.
 */
@ConfigMapping(prefix = "quarkus.flyway-mongodb")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface FlywayMongodbRuntimeConfig {

    /**
     * Per-MongoDB-client runtime configuration.
     */
    @ConfigDocMapKey("mongo-client-name")
    @ConfigDocSection
    @WithParentName
    @WithUnnamedKey(MongoConfig.DEFAULT_CLIENT_NAME)
    @WithDefaults
    Map<String, FlywayMongodbClientRuntimeConfig> clients();
}
