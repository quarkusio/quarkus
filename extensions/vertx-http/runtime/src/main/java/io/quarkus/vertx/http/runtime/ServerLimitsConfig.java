package io.quarkus.vertx.http.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.configuration.MemorySize;

@ConfigGroup
public class ServerLimitsConfig {
    /**
     * The maximum length of all headers.
     */
    @ConfigItem(defaultValue = "20K")
    public MemorySize maxHeaderSize;

    /**
     * The maximum size of a request body.
     * Default: 10240K.
     */
    @ConfigItem(defaultValue = "10240K")
    public Optional<MemorySize> maxBodySize;

    /**
     * The max HTTP chunk size
     */
    @ConfigItem
    public Optional<MemorySize> maxChunkSize;
}
