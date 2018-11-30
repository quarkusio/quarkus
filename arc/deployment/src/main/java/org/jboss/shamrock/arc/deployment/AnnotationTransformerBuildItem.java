package org.jboss.shamrock.arc.deployment;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.protean.arc.processor.AnnotationsTransformer;

public final class AnnotationTransformerBuildItem extends MultiBuildItem {

    private final AnnotationsTransformer transformer;

    public AnnotationTransformerBuildItem(AnnotationsTransformer transformer) {
        this.transformer = transformer;
    }

    public AnnotationsTransformer getAnnotationsTransformer() {
        return transformer;
    }

}
