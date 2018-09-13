package org.jboss.protean.arc.processor;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;

public class AnnotationTransformer {
    
    private List<BiFunction<AnnotationTarget, Collection<AnnotationInstance>, Collection<AnnotationInstance>>> transformers = null;
    
    Collection<AnnotationInstance> getAnnotations(AnnotationTarget target) {
        Collection<AnnotationInstance> annotations = null;
        for (BiFunction<AnnotationTarget, Collection<AnnotationInstance>, Collection<AnnotationInstance>> transformer : transformers) {
            annotations = transformer.apply(target, annotations);
        }
        return annotations;
    }

}
