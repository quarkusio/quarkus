package io.quarkus.arc.deployment;

import org.jboss.builder.item.MultiBuildItem;

import io.quarkus.arc.processor.AnnotationsTransformer;

public final class AnnotationsTransformerBuildItem extends MultiBuildItem {

    private final AnnotationsTransformer transformer;

    public AnnotationsTransformerBuildItem(AnnotationsTransformer transformer) {
        this.transformer = transformer;
    }

    public AnnotationsTransformer getAnnotationsTransformer() {
        return transformer;
    }

}
