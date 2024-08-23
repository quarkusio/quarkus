package io.quarkus.arc.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationOverlay;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

/**
 * Applies {@link AnnotationsTransformer}s and caches the results of transformations.
 *
 * @author Martin Kouba
 * @see AnnotationsTransformer
 */
public final class AnnotationStore {

    private final AnnotationOverlay delegate;

    AnnotationStore(IndexView index, Collection<AnnotationTransformation> transformations) {
        this.delegate = AnnotationOverlay.builder(index, transformations)
                .compatibleMode()
                .runtimeAnnotationsOnly()
                .build();
    }

    public AnnotationOverlay overlay() {
        return delegate;
    }

    /**
     * All {@link AnnotationsTransformer}s are applied and the result is cached.
     *
     * @param target
     * @return the annotation instance for the given target
     */
    public Collection<AnnotationInstance> getAnnotations(AnnotationTarget target) {
        return delegate.annotations(target.asDeclaration());
    }

    /**
     *
     * @param target
     * @param name
     * @return the annotation instance if present, {@code null} otherwise
     * @see #getAnnotations(AnnotationTarget)
     */
    public AnnotationInstance getAnnotation(AnnotationTarget target, DotName name) {
        return delegate.annotation(target.asDeclaration(), name);
    }

    /**
     *
     * @param target
     * @param name
     * @return {@code true} if the specified target contains the specified annotation, {@code false} otherwise
     * @see #getAnnotations(AnnotationTarget)
     */
    public boolean hasAnnotation(AnnotationTarget target, DotName name) {
        return delegate.hasAnnotation(target.asDeclaration(), name);
    }

    /**
     *
     * @param target
     * @param names
     * @return {@code true} if the specified target contains any of the specified annotations, {@code false} otherwise
     * @see #getAnnotations(AnnotationTarget)
     */
    public boolean hasAnyAnnotation(AnnotationTarget target, Iterable<DotName> names) {
        Set<DotName> set = new HashSet<>();
        for (DotName name : names) {
            set.add(name);
        }
        return delegate.hasAnyAnnotation(target.asDeclaration(), set);
    }

}
