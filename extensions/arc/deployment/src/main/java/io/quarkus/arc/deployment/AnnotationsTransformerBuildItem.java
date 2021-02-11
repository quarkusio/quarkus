package io.quarkus.arc.deployment;

import org.jboss.jandex.AnnotationTarget;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Make it possible to add, remove or alter annotations on various components.
 * The provided transformer uses {@link AnnotationsTransformer#appliesTo(AnnotationTarget.Kind)} to limit the scope
 * of transformer to classes, fields, methods, method params or a combination of those.
 *
 * These metadata changes are not stored in Jandex directly (Jandex is immutable) but instead in an abstraction
 * layer. Users/extensions can access {@link io.quarkus.arc.processor.AnnotationStore} to view the updated annotation
 * model.
 *
 * NOTE: Extensions that operate purely on Jandex index analysis won't be able to see any changes made via
 * {@link AnnotationsTransformer}!
 */
public final class AnnotationsTransformerBuildItem extends MultiBuildItem {

    private final AnnotationsTransformer transformer;

    public AnnotationsTransformerBuildItem(AnnotationsTransformer transformer) {
        this.transformer = transformer;
    }

    public AnnotationsTransformer getAnnotationsTransformer() {
        return transformer;
    }

}
