package io.quarkus.grpc.runtime.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * XDS config
 * * <a href="https://github.com/grpc/grpc-java/tree/master/examples/example-xds">XDS usage</a>
 */
@ConfigGroup
public class Xds {
    /**
     * Explicitly enable use of XDS.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    /**
     * Use secure credentials.
     */
    @ConfigItem(defaultValue = "false")
    public boolean secure;
}
