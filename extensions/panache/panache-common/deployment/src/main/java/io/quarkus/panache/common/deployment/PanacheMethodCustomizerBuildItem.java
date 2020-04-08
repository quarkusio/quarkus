package io.quarkus.panache.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item to declare that a {@link PanacheMethodCustomizer} should be used on Panache-enhanced methods.
 */
public final class PanacheMethodCustomizerBuildItem extends MultiBuildItem {
    private PanacheMethodCustomizer methodCustomizer;

    public PanacheMethodCustomizerBuildItem(PanacheMethodCustomizer methodCustomizer) {
        this.methodCustomizer = methodCustomizer;
    }

    public PanacheMethodCustomizer getMethodCustomizer() {
        return methodCustomizer;
    }
}
