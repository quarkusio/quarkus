package io.quarkus.arc.deployment;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
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
        return queryAndConditionallyFilter(target);
    }

    public AnnotationInstance getAnnotation(AnnotationTarget target, DotName annotationName) {
        if (target.kind().equals(AnnotationTarget.Kind.METHOD_PARAMETER)) {
            return queryForMethodParam(target, annotationName);
        } else {
            return beanDeployment.getAnnotation(target, annotationName);
        }
    }

    public AnnotationInstance getAnnotation(AnnotationTarget target, Class<? extends Annotation> annotationClass) {
        return getAnnotation(target, DotName.createSimple(annotationClass));
    }

    public boolean hasAnnotation(AnnotationTarget target, DotName annotationName) {
        if (target.kind().equals(AnnotationTarget.Kind.METHOD_PARAMETER)) {
            return queryForMethodParam(target, annotationName) != null;
        } else {
            return beanDeployment.hasAnnotation(target, annotationName);
        }
    }

    public boolean hasAnnotation(AnnotationTarget target, Class<? extends Annotation> annotationClass) {
        return hasAnnotation(target, DotName.createSimple(annotationClass));
    }

    @Override
    public Collection<AnnotationInstance> apply(AnnotationTarget target) {
        return queryAndConditionallyFilter(target);
    }

    private Collection<AnnotationInstance> queryAndConditionallyFilter(AnnotationTarget target) {
        if (target.kind().equals(AnnotationTarget.Kind.METHOD_PARAMETER)) {
            // We cannot query Jandex for method param. annotation target, so we operate on the whole method
            // and filter results accordingly
            Collection<AnnotationInstance> result = new ArrayList<>();
            for (AnnotationInstance instance : beanDeployment.getAnnotations(target.asMethodParameter().method())) {
                if (instance.target().kind().equals(AnnotationTarget.Kind.METHOD_PARAMETER)
                        && instance.target().asMethodParameter().position() == target.asMethodParameter().position()) {
                    result.add(instance);
                }
            }
            return result;
        } else {
            return beanDeployment.getAnnotations(target);
        }
    }

    private AnnotationInstance queryForMethodParam(AnnotationTarget target, DotName annotationName) {
        if (!target.kind().equals(AnnotationTarget.Kind.METHOD_PARAMETER)) {
            throw new IllegalArgumentException(
                    "TransformedAnnotationsBuildItem#queryForMethodParam needs to operate on METHOD_PARAMETER AnnotationTarget");
        }
        // We cannot query Jandex for method param. annotation target, so we operate on the whole method
        // and filter results accordingly
        for (AnnotationInstance instance : beanDeployment.getAnnotations(target.asMethodParameter().method())) {
            if (instance.name().equals(annotationName)
                    && instance.target().kind().equals(AnnotationTarget.Kind.METHOD_PARAMETER)
                    && instance.target().asMethodParameter().position() == target.asMethodParameter().position()) {
                return instance;
            }
        }
        return null;
    }
}
