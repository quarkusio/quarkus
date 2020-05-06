package io.quarkus.amazon.common.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SyncHttpClientBuildTimeConfig {

    /**
     * Type of the sync HTTP client implementation
     */
    @ConfigItem(defaultValue = "url")
    public SyncClientType type;

    public enum SyncClientType {
        URL,
        APACHE
    }
}
