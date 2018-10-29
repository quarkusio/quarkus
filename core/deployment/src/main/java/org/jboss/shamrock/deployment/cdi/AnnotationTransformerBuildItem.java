package org.jboss.shamrock.deployment.cdi;

import java.util.Collection;
import java.util.function.BiFunction;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;

public final class AnnotationTransformerBuildItem extends MultiBuildItem {

    final BiFunction<AnnotationTarget, Collection<AnnotationInstance>, Collection<AnnotationInstance>> transformer;

    public AnnotationTransformerBuildItem(BiFunction<AnnotationTarget, Collection<AnnotationInstance>, Collection<AnnotationInstance>> transformer) {
        this.transformer = transformer;
    }

    public BiFunction<AnnotationTarget, Collection<AnnotationInstance>, Collection<AnnotationInstance>> getTransformer() {
        return transformer;
    }
}
