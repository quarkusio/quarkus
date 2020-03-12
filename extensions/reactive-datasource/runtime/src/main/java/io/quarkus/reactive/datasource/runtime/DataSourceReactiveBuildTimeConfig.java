package io.quarkus.reactive.datasource.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * For now, the reactive extensions only support a default datasource.
 */
@ConfigRoot(name = "datasource.reactive", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class DataSourceReactiveBuildTimeConfig {

    /**
     * If we create a Reactive datasource for this datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT, defaultValue = "true")
    public boolean enabled;
}
