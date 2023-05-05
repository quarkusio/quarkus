package io.quarkus.datasource.runtime;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.datasource")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DataSourcesRuntimeConfig {

    /**
     * The default datasource.
     */
    @WithParentName
    DataSourceRuntimeConfig defaultDataSource();

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @WithParentName
    Map<String, DataSourceRuntimeConfig> namedDataSources();

    default DataSourceRuntimeConfig getDataSourceRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource();
        }

        DataSourceRuntimeConfig dataSourceRuntimeConfig = namedDataSources().get(dataSourceName);
        return Objects.requireNonNullElseGet(dataSourceRuntimeConfig, new Supplier<DataSourceRuntimeConfig>() {

            @Override
            public DataSourceRuntimeConfig get() {
                return ConfigUtils.getInitializedConfigGroup(DataSourceRuntimeConfig.class, "quarkus.datasource.*");
            }
        });
    }
}
