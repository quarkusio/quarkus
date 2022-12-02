package io.quarkus.datasource.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;

@ConfigGroup
public class DataSourceBuildTimeConfig {

    /**
     * The kind of database we will connect to (e.g. h2, postgresql...).
     */
    @ConfigItem
    @ConvertWith(DatabaseKindConverter.class)
    public Optional<String> dbKind = Optional.empty();

    /**
     * Configuration for DevServices. DevServices allows Quarkus to automatically start a database in dev and test mode.
     */
    @ConfigItem
    public DevServicesBuildTimeConfig devservices;

    /**
     * Whether this particular data source should be excluded from the health check if
     * the general health check for data sources is enabled.
     * <p>
     * By default, the health check includes all configured data sources (if it is enabled).
     */
    @ConfigItem(defaultValue = "false")
    public boolean healthExclude;

}
