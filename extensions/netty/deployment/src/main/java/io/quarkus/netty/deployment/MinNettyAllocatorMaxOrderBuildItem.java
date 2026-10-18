package io.quarkus.netty.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item to specify the minimal required `io.netty.allocator.maxOrder`.
 *
 * Quarkus by default uses `maxOrder == 1`. Extensions that require a larger value
 * can use this build item to specify it.
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
