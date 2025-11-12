package io.quarkus.deployment.dev;

import java.util.Map;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class AnnotationDependentClassesBuildItem extends MultiBuildItem {

    private final DotName annotationName;

    private final Map<DotName, Set<DotName>> dependencyToAnnotatedClasses;

    public AnnotationDependentClassesBuildItem(DotName annotationName,
            Map<DotName, Set<DotName>> dependencyToAnnotatedClasses) {
        this.annotationName = annotationName;
        this.dependencyToAnnotatedClasses = dependencyToAnnotatedClasses;
    }

    public DotName getAnnotationName() {
        return annotationName;
    }

    public Map<DotName, Set<DotName>> getDependencyToAnnotatedClasses() {
        return dependencyToAnnotatedClasses;
    }
}
