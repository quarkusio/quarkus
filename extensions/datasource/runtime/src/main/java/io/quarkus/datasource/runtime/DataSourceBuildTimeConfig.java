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

}
