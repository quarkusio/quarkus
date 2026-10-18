package io.quarkus.liquibase.mongodb.runtime;

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
 * Liquibase configuration supporting multiple clients.
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.liquibase-mongodb")
public interface LiquibaseMongodbBuildTimeConfig {

    /**
     * The liquibase configuration config by client name.
     */
    @ConfigDocMapKey("client-name")
    @ConfigDocSection
    @WithParentName
    @WithUnnamedKey(MongoConfig.DEFAULT_CLIENT_NAME)
    @WithDefaults
    Map<String, LiquibaseMongodbBuildTimeClientConfig> clientConfigs();
}
