package io.quarkus.reactive.mssql.client.runtime;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DataSourceReactiveMSSQLConfig {

    /**
     * The desired size (in bytes) for TDS packets.
     */
    OptionalInt packetSize();

    /**
     * Whether SSL/TLS is enabled.
     */
    @WithDefault("false")
    public boolean ssl();

}
