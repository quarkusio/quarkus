package io.quarkus.netty.deployment;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "netty", phase = ConfigPhase.BUILD_TIME)
public class NettyBuildTimeConfig {

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
    @ConfigItem
    public OptionalInt allocatorMaxOrder;
}
