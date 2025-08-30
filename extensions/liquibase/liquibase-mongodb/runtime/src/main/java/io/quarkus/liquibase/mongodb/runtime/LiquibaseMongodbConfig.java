package io.quarkus.liquibase.mongodb.runtime;

import java.util.Map;

import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.*;

/**
 * The liquibase configuration
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
     * Config by datasource name.
     */
    @ConfigDocMapKey("datasource-name")
    @ConfigDocSection
    @WithParentName
    @WithUnnamedKey(MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME)
    @WithDefaults
    Map<String, LiquibaseMongodbDataSourceConfig> dataSourceConfigs();
}
