package io.quarkus.datasource.runtime;

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
@ConfigRoot(name = "datasource", phase = ConfigPhase.RUN_TIME)
public class LegacyDataSourcesRuntimeConfig {

    /**
     * The default datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public LegacyDataSourceRuntimeConfig defaultDataSource;

    /**
     * Additional named datasources.
     */
    @ConfigDocSection
    @ConfigDocMapKey("datasource-name")
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, LegacyDataSourceRuntimeConfig> namedDataSources;
}
