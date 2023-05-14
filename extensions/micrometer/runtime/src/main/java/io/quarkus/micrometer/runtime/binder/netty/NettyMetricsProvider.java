package io.quarkus.micrometer.runtime.binder.netty;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.netty4.NettyAllocatorMetrics;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

@Singleton
public class NettyMetricsProvider {

    @Produces
    @Singleton
    public MeterBinder pooledByteBufAllocatorMetrics() {
        return new NettyAllocatorMetrics(PooledByteBufAllocator.DEFAULT);
    }

    @Produces
    @Singleton
    public MeterBinder unpooledByteBufAllocatorMetrics() {
        return new NettyAllocatorMetrics(UnpooledByteBufAllocator.DEFAULT);
    }

}
