package io.quarkus.micrometer.runtime.binder.netty;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.netty4.NettyAllocatorMetrics;
import io.netty.buffer.ByteBufAllocatorMetricProvider;
import io.vertx.core.buffer.impl.VertxByteBufAllocator;

@Singleton
public class VertxNettyAllocatorMetricsProvider {

    @Produces
    @Singleton
    public MeterBinder vertxPooledByteBufAllocatorMetrics() {
        return new NettyAllocatorMetrics((ByteBufAllocatorMetricProvider) VertxByteBufAllocator.POOLED_ALLOCATOR);
    }

    @Produces
    @Singleton
    public MeterBinder vertxUnpooledByteBufAllocatorMetrics() {
        return new NettyAllocatorMetrics((ByteBufAllocatorMetricProvider) VertxByteBufAllocator.UNPOOLED_ALLOCATOR);
    }

}
