package io.quarkus.reactive.mssql.client.runtime;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DataSourceReactiveMSSQLConfig {

    /**
     * The desired size (in bytes) for TDS packets.
     */
    @ConfigItem
    public OptionalInt packetSize = OptionalInt.empty();

    /**
     * Whether SSL/TLS is enabled.
     */
    @ConfigItem(defaultValue = "false")
    public boolean ssl = false;

}
