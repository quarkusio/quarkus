package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Make it possible to programmatically modify qualifiers on an injection point.
 */
public final class InjectionPointTransformerBuildItem extends MultiBuildItem {
    private final InjectionPointsTransformer transformer;

    public InjectionPointTransformerBuildItem(InjectionPointsTransformer transformer) {
        this.transformer = transformer;
    }

    public InjectionPointsTransformer getInjectionPointsTransformer() {
        return transformer;
    }
}
