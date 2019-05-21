package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.builder.item.MultiBuildItem;

public final class AnnotationsTransformerBuildItem extends MultiBuildItem {

    private final AnnotationsTransformer transformer;

    public AnnotationsTransformerBuildItem(AnnotationsTransformer transformer) {
        this.transformer = transformer;
    }

    public AnnotationsTransformer getAnnotationsTransformer() {
        return transformer;
    }

}
