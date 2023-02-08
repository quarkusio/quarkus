package io.quarkus.grpc.runtime.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * In-process config
 * * <a href="https://grpc.github.io/grpc-java/javadoc/io/grpc/inprocess/InProcessServerBuilder.html">in-process usage</a>
 */
@ConfigGroup
public class InProcess {
    /**
     * Explicitly enable use of in-progress.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    /**
     * Set in-progress name.
     */
    @ConfigItem(defaultValue = "quarkus-grpc")
    public String name;
}
