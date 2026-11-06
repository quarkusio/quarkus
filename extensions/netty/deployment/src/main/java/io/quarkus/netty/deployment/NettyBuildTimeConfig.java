package io.quarkus.netty.deployment;

import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.netty")
public interface NettyBuildTimeConfig {

    /**
     * Ignored. Since Netty 4.2, the default allocator is {@link io.netty.buffer.AdaptiveByteBufAllocator},
     * which has its own pooling strategy and ignores {@code maxOrder}; this property is no longer honored,
     * even when {@link io.netty.buffer.PooledByteBufAllocator} is selected explicitly.
     *
     * <p>
     * This property is retained only for compatibility, so that setting it does not fail the build.
     * It can be removed from configuration.
     *
     * @deprecated This property has no effect anymore.
     */
    @Deprecated(since = "4.0")
    OptionalInt allocatorMaxOrder();
}
