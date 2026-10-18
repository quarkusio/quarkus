package io.quarkus.resteasy.reactive.server.spi;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationStore;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationsTransformer;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Make it possible to add, remove or alter annotations on various components.
 * The provided transformer uses {@link AnnotationsTransformer#appliesTo(AnnotationTarget.Kind)} to limit the scope
 * of transformer to classes, fields, methods, method params or a combination of those.
 *
 * These metadata changes are not stored in Jandex directly (Jandex is immutable) but instead in an abstraction
 * layer. Users/extensions can access {@link AnnotationStore} to view the updated annotation
 * model.
 *
 * NOTE: Extensions that operate purely on Jandex index analysis won't be able to see any changes made via
 * {@link AnnotationsTransformer}!
 */
public final class AnnotationsTransformerBuildItem extends MultiBuildItem {

    private final AnnotationTransformation transformer;

    /**
     * @deprecated use {@link #AnnotationsTransformerBuildItem(AnnotationTransformation)}
     */
    @Deprecated(forRemoval = true)
    public AnnotationsTransformerBuildItem(AnnotationsTransformer transformer) {
        this.transformer = transformer;
    }

    public AnnotationsTransformerBuildItem(AnnotationTransformation transformation) {
        this.transformer = transformation;
    }

    /**
     * @deprecated use {@link #getAnnotationTransformation()}
     */
    @Deprecated(forRemoval = true)
    public AnnotationsTransformer getAnnotationsTransformer() {
        if (transformer instanceof AnnotationsTransformer) {
            return (AnnotationsTransformer) transformer;
        }
        throw new UnsupportedOperationException("AnnotationTransformation is not an AnnotationsTransformer: " + transformer);
    }

    public AnnotationTransformation getAnnotationTransformation() {
        return transformer;
    }

}
