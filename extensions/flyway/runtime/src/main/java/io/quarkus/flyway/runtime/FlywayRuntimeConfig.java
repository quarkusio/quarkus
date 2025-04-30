package io.quarkus.flyway.runtime;

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

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.flyway")
public interface FlywayRuntimeConfig {

    /**
     * Datasources.
     */
    @ConfigDocMapKey("datasource-name")
    @ConfigDocSection
    @WithParentName
    @WithUnnamedKey(DataSourceUtil.DEFAULT_DATASOURCE_NAME)
    @WithDefaults
    Map<String, FlywayDataSourceRuntimeConfig> datasources();
}
