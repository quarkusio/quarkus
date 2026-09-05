package io.quarkus.flyway.mongodb.runtime;

import java.util.Map;

import io.quarkus.mongodb.runtime.MongoConfig;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.flyway-mongodb")
public interface FlywayMongodbBuildTimeConfig {

    /**
     * Whether Flyway-MongoDB is enabled *during the build*.
     *
     * If Flyway-MongoDB is disabled, the Flyway-MongoDB beans won't be created and Flyway won't be usable.
     *
     * @asciidoclet
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Per-MongoDB-client configuration. The key is the MongoDB client name
     * (use the unnamed key for the default client).
     */
    @ConfigDocMapKey("mongo-client-name")
    @ConfigDocSection
    @WithParentName
    @WithUnnamedKey(MongoConfig.DEFAULT_CLIENT_NAME)
    @WithDefaults
    Map<String, FlywayMongodbClientBuildTimeConfig> clients();
}
