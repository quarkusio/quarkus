package io.quarkus.grpc.runtime.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.IgnoreProperty;

/**
 * In-process config
 * * <a href="https://grpc.github.io/grpc-java/javadoc/io/grpc/inprocess/InProcessServerBuilder.html">in-process usage</a>
 */
@ConfigGroup
public class InProcess implements Enabled {

    @Override
    @IgnoreProperty
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Explicitly enable use of in-process.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    /**
     * Set in-process name.
     */
    @ConfigItem(defaultValue = "quarkus-grpc")
    public String name;
}
