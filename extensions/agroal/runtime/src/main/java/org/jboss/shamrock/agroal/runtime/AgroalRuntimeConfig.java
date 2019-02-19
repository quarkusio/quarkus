package org.jboss.shamrock.agroal.runtime;

import java.util.Map;

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

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
