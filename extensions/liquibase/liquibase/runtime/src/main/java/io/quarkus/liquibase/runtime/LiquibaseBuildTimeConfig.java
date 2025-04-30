package io.quarkus.liquibase.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

/**
 * The liquibase build time configuration
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.liquibase")
public interface LiquibaseBuildTimeConfig {

    /**
     * Datasources.
     */
    @ConfigDocMapKey("datasource-name")
    @ConfigDocSection
    @WithParentName
    @WithUnnamedKey(DataSourceUtil.DEFAULT_DATASOURCE_NAME)
    @WithDefaults
    public Map<String, LiquibaseDataSourceBuildTimeConfig> datasources();
}
