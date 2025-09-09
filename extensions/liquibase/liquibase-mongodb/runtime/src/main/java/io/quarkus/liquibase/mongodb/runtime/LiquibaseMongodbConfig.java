package io.quarkus.liquibase.mongodb.runtime;

import java.util.Map;

import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

/**
 * Liquibase runtime configuration supporting multiple clients. This configuration includes the client name
 * used to connect to the database, defaulting to the default MongoDB client.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.liquibase-mongodb")
public interface LiquibaseMongodbConfig {

    /**
     * Flag to enable / disable Liquibase.
     *
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Config by client name.
     */
    @ConfigDocMapKey("client-name")
    @ConfigDocSection
    @WithParentName
    @WithUnnamedKey(MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME)
    @WithDefaults
    Map<String, LiquibaseMongodbClientConfig> clientConfigs();
}
