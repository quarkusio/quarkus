package org.jboss.resteasy.reactive.common.processor.transformation;

import java.util.Collection;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;

/**
 * Transformation context base for an {@link AnnotationsTransformation}.
 */
abstract class AnnotationsTransformationContext<C extends Collection<AnnotationInstance>> {

    protected final AnnotationTarget target;
    private C annotations;

    /**
     * 
     * @param target
     * @param annotations Mutable collection of annotations
     */
    public AnnotationsTransformationContext(AnnotationTarget target,
            C annotations) {
        this.target = target;
        this.annotations = annotations;
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

}
