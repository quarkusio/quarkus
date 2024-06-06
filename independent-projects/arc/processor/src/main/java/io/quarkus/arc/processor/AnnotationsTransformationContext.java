package io.quarkus.arc.processor;

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.BuildExtension.BuildContext;
import io.quarkus.arc.processor.BuildExtension.Key;

/**
 * Transformation context base for an {@link AnnotationsTransformation}.
 */
abstract class AnnotationsTransformationContext<C extends Collection<AnnotationInstance>> implements BuildContext {

    private static final Logger LOG = Logger.getLogger(AnnotationsTransformationContext.class);

    protected final BuildContext buildContext;
    protected final AnnotationTarget target;
    protected final AnnotationTarget methodParameterTarget;
    private C annotations;

    /**
     *
     * @param buildContext
     * @param target
     * @param annotations Mutable collection of annotations
     */
    public AnnotationsTransformationContext(BuildContext buildContext, AnnotationTarget target,
            AnnotationTarget methodParameterTarget,
            C annotations) {
        this.buildContext = buildContext;
        this.target = target;
        // once we remove #getTarget(), 'target' field should contain method parameter AnnotationTarget for method parameter injection
        // can be null for field injection as well as synth injection point
        this.methodParameterTarget = methodParameterTarget;
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

    public AnnotationTarget getAnnotationTarget() {
        return methodParameterTarget == null ? target : methodParameterTarget;
    }

    public C getAnnotations() {
        return annotations;
    }

    void setAnnotations(C annotations) {
        LOG.tracef("Annotations of %s transformed: %s", target, annotations);
        this.annotations = annotations;
    }

    public Collection<AnnotationInstance> getAllAnnotations() {
        return getAllAnnotationForTarget(getTarget());
    }

    public Collection<AnnotationInstance> getAllTargetAnnotations() {
        return getAllAnnotationForTarget(getAnnotationTarget());
    }

    private Collection<AnnotationInstance> getAllAnnotationForTarget(AnnotationTarget target) {
        AnnotationStore annotationStore = get(BuildExtension.Key.ANNOTATION_STORE);
        if (annotationStore == null) {
            throw new IllegalStateException(
                    "Attempted to use the getAllAnnotations() method but AnnotationStore wasn't initialized.");
        }
        // Jandex overlay in compatible mode won't allow us to query for METHOD_PARAMETER
        // hence if the "target" is method param, we query whole method and filter it
        if (AnnotationTarget.Kind.METHOD_PARAMETER.equals(target.kind())) {
            return Annotations.getParameterAnnotations(at -> annotationStore.getAnnotations(at),
                    target.asMethodParameter().method(), target.asMethodParameter().position());
        } else {
            return annotationStore.getAnnotations(target);
        }
    }

}
