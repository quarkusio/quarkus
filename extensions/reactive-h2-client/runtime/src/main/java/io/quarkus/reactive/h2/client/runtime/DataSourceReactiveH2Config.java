package io.quarkus.reactive.h2.client.runtime;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DataSourceReactiveH2Config {

    /**
     * Connection timeout in seconds
     */
    @ConfigItem()
    public OptionalInt connectionTimeout = OptionalInt.empty();
}
