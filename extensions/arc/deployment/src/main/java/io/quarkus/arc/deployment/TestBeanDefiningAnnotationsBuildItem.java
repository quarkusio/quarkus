package io.quarkus.arc.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds the names of all annotations that mark a class as a Quarkus test and therefore turn it into a
 * CDI bean. This includes the seed test annotations (e.g. {@code @QuarkusTest}) as well as any
 * annotation (transitively) composed with them, discovered against the application index.
 */
final class TestBeanDefiningAnnotationsBuildItem extends SimpleBuildItem {

    private final Set<String> annotationNames;

    TestBeanDefiningAnnotationsBuildItem(Set<String> annotationNames) {
        this.annotationNames = annotationNames;
    }

    Set<String> getAnnotationNames() {
        return annotationNames;
    }
}
