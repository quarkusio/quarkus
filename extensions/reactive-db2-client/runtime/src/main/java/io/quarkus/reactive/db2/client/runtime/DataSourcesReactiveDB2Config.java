package io.quarkus.reactive.db2.client.runtime;

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
public interface DataSourcesReactiveDB2Config {

    /**
     * Datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(DataSourceUtil.DEFAULT_DATASOURCE_NAME)
    Map<String, DataSourceReactiveDB2OuterNamedConfig> dataSources();

    @ConfigGroup
    public interface DataSourceReactiveDB2OuterNamedConfig {

        /**
         * The DB2-specific configuration.
         */
        DataSourceReactiveDB2OuterNestedNamedConfig reactive();
    }

    @ConfigGroup
    public interface DataSourceReactiveDB2OuterNestedNamedConfig {

        /**
         * The DB2-specific configuration.
         */
        DataSourceReactiveDB2Config db2();
    }

}
