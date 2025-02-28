package io.quarkus.micrometer.runtime.binder.netty;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.netty.buffer.ByteBufAllocatorMetricProvider;
import io.vertx.core.buffer.impl.VertxByteBufAllocator;

@Singleton
public class VertxNettyAllocatorMetricsProvider {

    /**
     * The name of the Vert.x pooled allocator.
     */
    public static final String VERTX_POOLED_ALLOCATOR_NAME = "vertx-pooled";

    /**
     * The name of the Vert.x unpooled allocator.
     */
    public static final String VERTX_UNPOOLED_ALLOCATOR_NAME = "vertx-unpooled";

    @Produces
    @Singleton
    public MeterBinder vertxPooledByteBufAllocatorMetrics() {
        return new NettyAllocatorMetrics(VERTX_POOLED_ALLOCATOR_NAME,
                (ByteBufAllocatorMetricProvider) VertxByteBufAllocator.POOLED_ALLOCATOR);
    }

    @Produces
    @Singleton
    public MeterBinder vertxUnpooledByteBufAllocatorMetrics() {
        return new NettyAllocatorMetrics(VERTX_UNPOOLED_ALLOCATOR_NAME,
                (ByteBufAllocatorMetricProvider) VertxByteBufAllocator.UNPOOLED_ALLOCATOR);
    }

}
