package io.quarkus.arc.deployment;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanDeployment;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Makes it possible to query transformed annotations for a given annotation target.
 *
 * @see AnnotationsTransformer
 */
public final class TransformedAnnotationsBuildItem extends SimpleBuildItem
        implements Function<AnnotationTarget, Collection<AnnotationInstance>> {

    private final BeanDeployment beanDeployment;

    TransformedAnnotationsBuildItem(BeanDeployment beanDeployment) {
        this.beanDeployment = beanDeployment;
    }

    public Collection<AnnotationInstance> getAnnotations(AnnotationTarget target) {
        return beanDeployment.getAnnotations(target);
    }

    public AnnotationInstance getAnnotation(AnnotationTarget target, DotName annotationName) {
        return beanDeployment.getAnnotation(target, annotationName);
    }

    public AnnotationInstance getAnnotation(AnnotationTarget target, Class<? extends Annotation> annotationClass) {
        return getAnnotation(target, DotName.createSimple(annotationClass));
    }

    public boolean hasAnnotation(AnnotationTarget target, DotName annotationName) {
        return beanDeployment.hasAnnotation(target, annotationName);
    }

    public boolean hasAnnotation(AnnotationTarget target, Class<? extends Annotation> annotationClass) {
        return hasAnnotation(target, DotName.createSimple(annotationClass));
    }

    @Override
    public Collection<AnnotationInstance> apply(AnnotationTarget target) {
        return beanDeployment.getAnnotations(target);
    }

}
