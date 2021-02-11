package io.quarkus.reactive.datasource.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DataSourceReactiveBuildTimeConfig {

    /**
     * If we create a Reactive datasource for this datasource.
     */
    @ConfigItem(name = ConfigItem.PARENT, defaultValue = "true")
    public boolean enabled = true;
}
