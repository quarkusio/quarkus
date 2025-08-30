package io.quarkus.liquibase.mongodb.runtime;

import java.util.Map;

import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

/**
 * The liquibase configuration
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.liquibase-mongodb")
public interface LiquibaseMongodbBuildTimeConfig {

    /**
     * Config by datasource.
     */
    @ConfigDocMapKey("datasource-name")
    @ConfigDocSection
    @WithParentName
    @WithUnnamedKey(MongoClientBeanUtil.DEFAULT_MONGOCLIENT_NAME)
    @WithDefaults
    Map<String, LiquibaseMongodbBuildTimeDataSourceConfig> dataSourceConfigs();
}
