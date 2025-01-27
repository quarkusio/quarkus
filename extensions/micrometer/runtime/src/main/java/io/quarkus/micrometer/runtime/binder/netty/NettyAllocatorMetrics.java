package io.quarkus.micrometer.runtime.binder.netty;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.ByteBufAllocatorMetricProvider;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocatorMetric;

/**
 * {@link MeterBinder} for Netty memory allocators.
 * <p>
 * This class is based on the MicroMeter NettyAllocatorMetrics class, but remove the "id" from the tags are it's
 * computed from the `hashCode` which does not allow aggregation across processed.
 * Instead, it gets a {@code name} label indicating an unique name for the allocator.
 */
public class NettyAllocatorMetrics implements MeterBinder {

    private final ByteBufAllocatorMetricProvider allocator;
    private final String name;

    /**
     * Create a binder instance for the given allocator.
     *
     * @param name the unique name for the allocator
     * @param allocator the {@code ByteBuf} allocator to instrument
     */
    public NettyAllocatorMetrics(String name, ByteBufAllocatorMetricProvider allocator) {
        this.name = name;
        this.allocator = allocator;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        ByteBufAllocatorMetric allocatorMetric = this.allocator.metric();
        Tags tags = Tags.of(
                NettyMeters.AllocatorKeyNames.NAME.asString(), this.name,
                NettyMeters.AllocatorKeyNames.ALLOCATOR_TYPE.asString(), this.allocator.getClass().getSimpleName());

        Gauge
                .builder(NettyMeters.ALLOCATOR_MEMORY_USED.getName(), allocatorMetric,
                        ByteBufAllocatorMetric::usedHeapMemory)
                .tags(tags.and(NettyMeters.AllocatorMemoryKeyNames.MEMORY_TYPE.asString(), "heap"))
                .register(registry);

        Gauge
                .builder(NettyMeters.ALLOCATOR_MEMORY_USED.getName(), allocatorMetric,
                        ByteBufAllocatorMetric::usedDirectMemory)
                .tags(tags.and(NettyMeters.AllocatorMemoryKeyNames.MEMORY_TYPE.asString(), "direct"))
                .register(registry);

        if (this.allocator instanceof PooledByteBufAllocator pooledByteBufAllocator) {
            PooledByteBufAllocatorMetric pooledAllocatorMetric = pooledByteBufAllocator.metric();

            Gauge
                    .builder(NettyMeters.ALLOCATOR_MEMORY_PINNED.getName(), pooledByteBufAllocator,
                            PooledByteBufAllocator::pinnedHeapMemory)
                    .tags(tags.and(NettyMeters.AllocatorMemoryKeyNames.MEMORY_TYPE.asString(), "heap"))
                    .register(registry);

            Gauge
                    .builder(NettyMeters.ALLOCATOR_MEMORY_PINNED.getName(), pooledByteBufAllocator,
                            PooledByteBufAllocator::pinnedDirectMemory)
                    .tags(tags.and(NettyMeters.AllocatorMemoryKeyNames.MEMORY_TYPE.asString(), "direct"))
                    .register(registry);

            Gauge
                    .builder(NettyMeters.ALLOCATOR_POOLED_ARENAS.getName(), pooledAllocatorMetric,
                            PooledByteBufAllocatorMetric::numHeapArenas)
                    .tags(tags.and(NettyMeters.AllocatorMemoryKeyNames.MEMORY_TYPE.asString(), "heap"))
                    .register(registry);
            Gauge
                    .builder(NettyMeters.ALLOCATOR_POOLED_ARENAS.getName(), pooledAllocatorMetric,
                            PooledByteBufAllocatorMetric::numDirectArenas)
                    .tags(tags.and(NettyMeters.AllocatorMemoryKeyNames.MEMORY_TYPE.asString(), "direct"))
                    .register(registry);

            Gauge
                    .builder(NettyMeters.ALLOCATOR_POOLED_CACHE_SIZE.getName(), pooledAllocatorMetric,
                            PooledByteBufAllocatorMetric::normalCacheSize)
                    .tags(tags.and(NettyMeters.AllocatorPooledCacheKeyNames.CACHE_TYPE.asString(), "normal"))
                    .register(registry);
            Gauge
                    .builder(NettyMeters.ALLOCATOR_POOLED_CACHE_SIZE.getName(), pooledAllocatorMetric,
                            PooledByteBufAllocatorMetric::smallCacheSize)
                    .tags(tags.and(NettyMeters.AllocatorPooledCacheKeyNames.CACHE_TYPE.asString(), "small"))
                    .register(registry);

            Gauge
                    .builder(NettyMeters.ALLOCATOR_POOLED_THREADLOCAL_CACHES.getName(), pooledAllocatorMetric,
                            PooledByteBufAllocatorMetric::numThreadLocalCaches)
                    .tags(tags)
                    .register(registry);

            Gauge
                    .builder(NettyMeters.ALLOCATOR_POOLED_CHUNK_SIZE.getName(), pooledAllocatorMetric,
                            PooledByteBufAllocatorMetric::chunkSize)
                    .tags(tags)
                    .register(registry);
        }
    }

}
