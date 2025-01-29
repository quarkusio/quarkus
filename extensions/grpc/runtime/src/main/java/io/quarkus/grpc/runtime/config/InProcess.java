package io.quarkus.grpc.runtime.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * In-process config
 * * <a href="https://grpc.github.io/grpc-java/javadoc/io/grpc/inprocess/InProcessServerBuilder.html">in-process usage</a>
 */
@ConfigGroup
public interface InProcess extends Enabled {

    /**
     * Explicitly enable use of in-process.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Set in-process name.
     */
    @WithDefault("quarkus-grpc")
    String name();
}
