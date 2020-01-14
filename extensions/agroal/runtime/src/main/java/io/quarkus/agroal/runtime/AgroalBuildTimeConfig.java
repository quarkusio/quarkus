package io.quarkus.agroal.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class AgroalBuildTimeConfig {

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
     * Whether or not an health check is published in case the smallrye-health extension is present
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean healthEnabled;

    /**
     * Whether or not datasource metrics are published in case the smallrye-metrics extension is present (default to false).
     * NOTE: This is different from the "enable-metrics" property that needs to be set on data source level to enable
     * collection of metrics for that data source.
     */
    @ConfigItem(name = "metrics.enabled", defaultValue = "false")
    public boolean metricsEnabled;

}
