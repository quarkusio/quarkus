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
     * <p>
     * This property only affects the {@link io.netty.buffer.PooledByteBufAllocator}.
     * Since Netty 4.2, the default allocator is {@link io.netty.buffer.AdaptiveByteBufAllocator},
     * which has its own pooling strategy and ignores {@code maxOrder}.
     *
     * <p>
     * Configuring this property overrides the minimum {@code maxOrder} requested by the extensions.
     *
     * <p>
     * This property affects the memory consumption of the application.
     * It must be used carefully.
     * More details on https://programmer.group/pool-area-of-netty-memory-pool.html.
     *
     * @see io.netty.buffer.PooledByteBufAllocator
     */
    OptionalInt allocatorMaxOrder();
}
