package io.quarkus.micrometer.runtime.binder.netty;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufAllocatorMetricProvider;

@Singleton
public class NettyMetricsProvider {

    public static final String NETTY_DEFAULT_ALLOCATOR_NAME = "default";

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
        return new NettyAllocatorMetrics(NETTY_DEFAULT_ALLOCATOR_NAME,
                (ByteBufAllocatorMetricProvider) ByteBufAllocator.DEFAULT);
    }

}
