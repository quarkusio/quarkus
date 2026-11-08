package io.quarkus.micrometer.runtime.binder.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

/**
 * {@link NettyMetricsProvider} is only used when Vert.x is absent, which the Quarkus tests of this extension
 * cannot reproduce, so it is tested directly.
 */
class NettyMetricsProviderTest {

    private final NettyMetricsProvider provider = new NettyMetricsProvider();

    @Test
    void defaultAllocatorIsTracked() {
        NettyAllocatorMetrics metrics = assertInstanceOf(NettyAllocatorMetrics.class,
                provider.defaultByteBufAllocatorMetrics());
        assertSame(ByteBufAllocator.DEFAULT, metrics.getAllocator());
    }

    @Test
    void unpooledAllocatorIsTrackedWhenNotTheDefault() {
        // With Netty's defaults, the default allocator is adaptive, so the unpooled one needs its own binder
        assertNotSame(UnpooledByteBufAllocator.DEFAULT, ByteBufAllocator.DEFAULT);
        NettyAllocatorMetrics metrics = assertInstanceOf(NettyAllocatorMetrics.class,
                provider.unpooledByteBufAllocatorMetrics());
        assertSame(UnpooledByteBufAllocator.DEFAULT, metrics.getAllocator());
    }

    @Test
    void bindersCanAlwaysBeBound() {
        // All produced binders are bound, so none may be null
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        for (MeterBinder binder : new MeterBinder[] { provider.defaultByteBufAllocatorMetrics(),
                provider.unpooledByteBufAllocatorMetrics() }) {
            assertNotNull(binder);
            binder.bindTo(registry);
        }
        assertEquals(2, registry.find(NettyMeters.ALLOCATOR_MEMORY_USED.getName())
                .tag(NettyMeters.AllocatorKeyNames.NAME.asString(), NettyMetricsProvider.NETTY_UNPOOLED_ALLOCATOR_NAME)
                .gauges().size());
    }
}
