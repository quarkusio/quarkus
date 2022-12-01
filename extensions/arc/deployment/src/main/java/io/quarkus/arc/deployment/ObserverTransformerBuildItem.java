package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.ObserverTransformer;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item is used to register an {@link ObserverTransformer} instance.
 */
public final class ObserverTransformerBuildItem extends MultiBuildItem {

    private final ObserverTransformer transformer;

    public ObserverTransformerBuildItem(ObserverTransformer transformer) {
        this.transformer = transformer;
    }

    public ObserverTransformer getInstance() {
        return transformer;
    }
}
