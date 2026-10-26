package io.quarkus.netty.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item to specify the minimal required {@code io.netty.allocator.maxOrder}.
 *
 * Quarkus by default uses {@code maxOrder == 3}. Extensions that require a larger
 * value can use this build item to specify it.
 *
 * <p>
 * <strong>Note:</strong> {@code io.netty.allocator.maxOrder} only affects
 * {@link io.netty.buffer.PooledByteBufAllocator}. Since Netty 4.2, the default
 * allocator is {@link io.netty.buffer.AdaptiveByteBufAllocator}, which ignores
 * this property.
 */
public final class MinNettyAllocatorMaxOrderBuildItem extends MultiBuildItem {
    private final int maxOrder;

    public MinNettyAllocatorMaxOrderBuildItem(int maxOrder) {
        this.maxOrder = maxOrder;
    }

    public int getMaxOrder() {
        return maxOrder;
    }
}
