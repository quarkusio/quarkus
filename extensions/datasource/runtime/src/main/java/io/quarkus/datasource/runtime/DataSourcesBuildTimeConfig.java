package io.quarkus.datasource.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class DataSourcesBuildTimeConfig {

    /**
     * The default datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public DataSourceBuildTimeConfig defaultDataSource;

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, DataSourceBuildTimeConfig> namedDataSources;

    /**
     * Whether or not an health check is published in case the smallrye-health extension is present.
     * <p>
     * This is a global setting and is not specific to a datasource.
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean healthEnabled;

    /**
     * Whether or not datasource metrics are published in case a metrics extension is present.
     * <p>
     * This is a global setting and is not specific to a datasource.
     * <p>
     * NOTE: This is different from the "jdbc.enable-metrics" property that needs to be set on the JDBC datasource level to
     * enable collection of metrics for that datasource.
     */
    @ConfigItem(name = "metrics.enabled")
    public boolean metricsEnabled;

    /**
     * Only here to detect configuration errors.
     * <p>
     * This used to be runtime but we don't really care, we just want to catch invalid configurations.
     *
     * @deprecated
     */
    @Deprecated
    public Optional<String> url;

    /**
     * Only here to detect configuration errors.
     *
     * @deprecated
     */
    @Deprecated
    public Optional<String> driver;

    public DataSourceBuildTimeConfig getDataSourceRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return defaultDataSource;
        }

        DataSourceBuildTimeConfig dataSourceBuildTimeConfig = namedDataSources.get(dataSourceName);
        if (dataSourceBuildTimeConfig == null) {
            return new DataSourceBuildTimeConfig();
        }

        return dataSourceBuildTimeConfig;
    }

}
