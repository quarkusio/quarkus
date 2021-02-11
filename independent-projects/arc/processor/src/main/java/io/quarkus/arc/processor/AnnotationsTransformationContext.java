package io.quarkus.arc.processor;

import io.quarkus.arc.processor.BuildExtension.BuildContext;
import io.quarkus.arc.processor.BuildExtension.Key;
import java.util.Collection;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;

/**
 * Transformation context base for an {@link AnnotationsTransformation}.
 */
abstract class AnnotationsTransformationContext<C extends Collection<AnnotationInstance>> implements BuildContext {

    protected final BuildContext buildContext;
    protected final AnnotationTarget target;
    private C annotations;

    /**
     * 
     * @param buildContext
     * @param target
     * @param annotations Mutable collection of annotations
     */
    public AnnotationsTransformationContext(BuildContext buildContext, AnnotationTarget target,
            C annotations) {
        this.buildContext = buildContext;
        this.target = target;
        this.annotations = annotations;
    }

    @Override
    public <V> V get(Key<V> key) {
        return buildContext.get(key);
    }

    @Override
    public <V> V put(Key<V> key, V value) {
        return buildContext.put(key, value);
    }

    public AnnotationTarget getTarget() {
        return target;
    }

    public C getAnnotations() {
        return annotations;
    }

    void setAnnotations(C annotations) {
        this.annotations = annotations;
    }

    public Collection<AnnotationInstance> getAllAnnotations() {
        AnnotationStore annotationStore = get(BuildExtension.Key.ANNOTATION_STORE);
        if (annotationStore == null) {
            throw new IllegalStateException(
                    "Attempted to use the getAllAnnotations() method but AnnotationStore wasn't initialized.");
        }
        return annotationStore.getAnnotations(getTarget());
    }

}
