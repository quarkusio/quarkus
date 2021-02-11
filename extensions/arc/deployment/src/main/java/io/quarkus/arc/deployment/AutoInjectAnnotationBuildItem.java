package io.quarkus.arc.deployment;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item can be used to define annotations that will turn a non-static field into an injection point even if no
 * {@link Inject} is declared.
 * 
 * @see AutoInjectFieldProcessor
 */
public final class AutoInjectAnnotationBuildItem extends MultiBuildItem {

    private final List<DotName> annotationNames;

    public AutoInjectAnnotationBuildItem(DotName... annotationNames) {
        this.annotationNames = Arrays.asList(annotationNames);
    }

    public AutoInjectAnnotationBuildItem(List<DotName> annotationNames) {
        this.annotationNames = annotationNames;
    }

    public List<DotName> getAnnotationNames() {
        return annotationNames;
    }

}
