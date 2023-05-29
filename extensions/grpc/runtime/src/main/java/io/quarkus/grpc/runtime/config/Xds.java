package io.quarkus.grpc.runtime.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.IgnoreProperty;

/**
 * XDS config
 * * <a href="https://github.com/grpc/grpc-java/tree/master/examples/example-xds">XDS usage</a>
 */
@ConfigGroup
public class Xds implements Enabled {

    @Override
    @IgnoreProperty
    public boolean isEnabled() {
        return enabled;
    }

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
