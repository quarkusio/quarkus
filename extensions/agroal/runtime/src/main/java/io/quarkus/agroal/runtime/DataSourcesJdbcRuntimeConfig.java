package io.quarkus.agroal.runtime;

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

@ConfigMapping(prefix = "quarkus.datasource")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DataSourcesJdbcRuntimeConfig {

    /**
     * The default datasource.
     */
    DataSourceJdbcRuntimeConfig jdbc();

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @WithParentName
    @WithDefaults
    Map<String, DataSourceJdbcOuterNamedRuntimeConfig> namedDataSources();

    @ConfigGroup
    public interface DataSourceJdbcOuterNamedRuntimeConfig {

        /**
         * The JDBC runtime configuration.
         */
        DataSourceJdbcRuntimeConfig jdbc();
    }

    default DataSourceJdbcRuntimeConfig getDataSourceJdbcRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return jdbc();
        }

        return namedDataSources().get(dataSourceName).jdbc();
    }
}
