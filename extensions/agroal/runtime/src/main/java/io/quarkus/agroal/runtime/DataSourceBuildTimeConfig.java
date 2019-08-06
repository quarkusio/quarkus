package io.quarkus.agroal.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DataSourceBuildTimeConfig {

    /**
     * The datasource driver class name. Example `org.h2.Driver`, `com.mysql.jdbc.Driver`
     */
    @ConfigItem
    public Optional<String> driver;

    /**
     * Whether we want to use XA.
     *
     * If used, the driver has to support it.
     */
    @ConfigItem(defaultValue = "false")
    public boolean xa;

}
