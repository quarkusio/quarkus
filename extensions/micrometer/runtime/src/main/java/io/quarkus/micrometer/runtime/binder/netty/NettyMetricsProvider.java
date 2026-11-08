package io.quarkus.micrometer.runtime.binder.netty;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufAllocatorMetricProvider;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 * Tracks Netty's shared allocators. Only registered when Vert.x is absent, as otherwise these allocators are
 * already tracked by {@link VertxNettyAllocatorMetricsProvider}.
 */
@Singleton
public class NettyMetricsProvider {

    /**
     * The name of Netty's default allocator, {@code ByteBufAllocator.DEFAULT}.
     */
    public static final String NETTY_DEFAULT_ALLOCATOR_NAME = "default";

    /**
     * The name of Netty's shared unpooled allocator, {@code UnpooledByteBufAllocator.DEFAULT}.
     */
    public static final String NETTY_UNPOOLED_ALLOCATOR_NAME = "unpooled";

    /**
     * Binders are iterated and bound unconditionally, so producers must not return {@code null}.
     */
    private static final MeterBinder NO_OP = registry -> {
    };

    /**
     * {@code ByteBufAllocator.DEFAULT} is the allocator Netty channels actually use unless told otherwise, not
     * merely an unused fallback: it resolves at classload, depending on the {@code io.netty.allocator.type}
     * system property, to {@code PooledByteBufAllocator}, {@code UnpooledByteBufAllocator} or (since Netty 4.2,
     * the common case here) {@code AdaptiveByteBufAllocator}; all three implement
     * {@link ByteBufAllocatorMetricProvider}.
     */
    @Produces
    @Singleton
    public MeterBinder defaultByteBufAllocatorMetrics() {
        if (!(ByteBufAllocator.DEFAULT instanceof ByteBufAllocatorMetricProvider allocator)) {
            Logger.getLogger(NettyMetricsProvider.class).warnf(
                    "Unable to report Netty allocator metrics: %s does not implement %s",
                    ByteBufAllocator.DEFAULT.getClass().getName(), ByteBufAllocatorMetricProvider.class.getName());
            return NO_OP;
        }
        return new NettyAllocatorMetrics(NETTY_DEFAULT_ALLOCATOR_NAME, allocator);
    }

    /**
     * {@code UnpooledByteBufAllocator.DEFAULT} is used even when it is not the default allocator, e.g. by the
     * {@code io.netty.buffer.Unpooled} helpers. When it is the default allocator, it is already tracked by
     * {@link #defaultByteBufAllocatorMetrics()}.
     */
    @Produces
    @Singleton
    public MeterBinder unpooledByteBufAllocatorMetrics() {
        if (UnpooledByteBufAllocator.DEFAULT == ByteBufAllocator.DEFAULT) {
            return NO_OP;
        }
        return new NettyAllocatorMetrics(NETTY_UNPOOLED_ALLOCATOR_NAME, UnpooledByteBufAllocator.DEFAULT);
    }

}
