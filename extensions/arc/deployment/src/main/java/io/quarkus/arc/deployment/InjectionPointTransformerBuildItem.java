package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.builder.item.MultiBuildItem;

public final class InjectionPointTransformerBuildItem extends MultiBuildItem {
    private final InjectionPointsTransformer transformer;

    public InjectionPointTransformerBuildItem(InjectionPointsTransformer transformer) {
        this.transformer = transformer;
    }

    public InjectionPointsTransformer getInjectionPointsTransformer() {
        return transformer;
    }
}
