package io.quarkus.micrometer.runtime.binder.netty;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

@Singleton
public class NettyMetricsProvider {

    public static final String NETTY_DEFAULT_POOLED_ALLOCATOR_NAME = "pooled";

    public static final String NETTY_DEFAULT_UNPOOLED_ALLOCATOR_NAME = "unpooled";

    @Produces
    @Singleton
    public MeterBinder pooledByteBufAllocatorMetrics() {
        return new NettyAllocatorMetrics(NETTY_DEFAULT_POOLED_ALLOCATOR_NAME, PooledByteBufAllocator.DEFAULT);
    }

    @Produces
    @Singleton
    public MeterBinder unpooledByteBufAllocatorMetrics() {
        return new NettyAllocatorMetrics(NETTY_DEFAULT_UNPOOLED_ALLOCATOR_NAME, UnpooledByteBufAllocator.DEFAULT);
    }

}
