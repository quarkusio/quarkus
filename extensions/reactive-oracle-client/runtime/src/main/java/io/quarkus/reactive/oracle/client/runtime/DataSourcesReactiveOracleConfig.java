package io.quarkus.reactive.oracle.client.runtime;

import java.util.Map;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.datasource")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DataSourcesReactiveOracleConfig {

    /**
     * Datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(DataSourceUtil.DEFAULT_DATASOURCE_NAME)
    Map<String, DataSourceReactiveOracleOuterNamedConfig> dataSources();

    @ConfigGroup
    public interface DataSourceReactiveOracleOuterNamedConfig {

        /**
         * The Oracle-specific configuration.
         */
        DataSourceReactiveOracleOuterNestedNamedConfig reactive();
    }

    @ConfigGroup
    public interface DataSourceReactiveOracleOuterNestedNamedConfig {

        /**
         * The Oracle-specific configuration.
         */
        DataSourceReactiveOracleConfig oracle();
    }

}
