package io.quarkus.agroal.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * This configuration class is here for compatibility reason and is planned for removal.
 */
@Deprecated
@ConfigRoot(name = "datasource", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LegacyDataSourcesJdbcBuildTimeConfig {

    /**
     * The default datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public LegacyDataSourceJdbcBuildTimeConfig defaultDataSource;

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, LegacyDataSourceJdbcBuildTimeConfig> namedDataSources;
}
