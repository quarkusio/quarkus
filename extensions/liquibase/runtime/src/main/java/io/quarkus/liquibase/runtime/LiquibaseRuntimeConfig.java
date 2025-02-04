package io.quarkus.liquibase.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
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
 * Liquibase runtime configuration.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.liquibase")
public interface LiquibaseRuntimeConfig {

    /**
     * Flag to enable / disable Liquibase.
     *
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Datasources.
     */
    @ConfigDocMapKey("datasource-name")
    @ConfigDocSection
    @WithParentName
    @WithUnnamedKey(DataSourceUtil.DEFAULT_DATASOURCE_NAME)
    @WithDefaults
    public Map<String, LiquibaseDataSourceRuntimeConfig> datasources();
}
