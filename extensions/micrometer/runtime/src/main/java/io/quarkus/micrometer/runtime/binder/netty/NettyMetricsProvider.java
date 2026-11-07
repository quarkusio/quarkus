package io.quarkus.micrometer.runtime.binder.netty;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufAllocatorMetricProvider;

@Singleton
public class NettyMetricsProvider {

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
            return null;
        }
        return new NettyAllocatorMetrics("default", allocator);
    }

}
