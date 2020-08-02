package io.quarkus.arc.deployment;

import java.util.Collection;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;

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

    private final Function<AnnotationTarget, Collection<AnnotationInstance>> fun;

    TransformedAnnotationsBuildItem(BeanDeployment beanDeployment) {
        this.fun = beanDeployment::getAnnotations;
    }

    public Collection<AnnotationInstance> getAnnotations(AnnotationTarget target) {
        return fun.apply(target);
    }

    @Override
    public Collection<AnnotationInstance> apply(AnnotationTarget target) {
        return fun.apply(target);
    }

}
