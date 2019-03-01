package io.quarkus.agroal.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.RUN_TIME)
public class AgroalRuntimeConfig {

    /**
     * The default datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public DataSourceRuntimeConfig defaultDataSource;

    /**
     * Additional datasources.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, DataSourceRuntimeConfig> namedDataSources;
}
