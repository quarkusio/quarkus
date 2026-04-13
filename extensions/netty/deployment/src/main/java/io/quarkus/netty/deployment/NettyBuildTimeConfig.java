package io.quarkus.netty.deployment;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.netty")
public interface NettyBuildTimeConfig {

    /**
     * The value configuring the {@code io.netty.allocator.maxOrder} system property of Netty.
     * The default value is {@code 3}.
     *
     * Configuring this property overrides the minimum {@code maxOrder} requested by the extensions.
     *
     * This property affects the memory consumption of the application.
     * It must be used carefully.
     * More details on https://programmer.group/pool-area-of-netty-memory-pool.html.
     */
    OptionalInt allocatorMaxOrder();

    /**
     * The number of heap arenas used by the Netty allocator.
     * <p>
     * When not configured, the default value is computed at runtime based on the number of available processors,
     * capped to avoid excessive memory usage on machines with many cores:
     * {@code 2 * min(available processors, 16)}.
     */
    OptionalInt allocatorNumHeapArenas();

    /**
     * The number of direct arenas used by the Netty allocator.
     * <p>
     * When not configured, the default value is computed at runtime based on the number of available processors,
     * capped to avoid excessive memory usage on machines with many cores:
     * {@code 2 * min(available processors, 16)}.
     */
    OptionalInt allocatorNumDirectArenas();
}
