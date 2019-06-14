package io.quarkus.agroal.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DataSourceBuildTimeConfig {

    /**
     * The datasource driver class name
     */
    @ConfigItem
    public Optional<String> driver;

    /**
     * Whether we want to use XA.
     * <p>
     * If used, the driver has to support it.
     */
    @ConfigItem(defaultValue = "false")
    public boolean xa;

}
