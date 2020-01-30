package io.quarkus.deployment.builditem.nativeimage;

import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used by {@code io.quarkus.deployment.steps.ReflectiveHierarchyStep} to determine whether or
 * not the final fields of the class should be writable (which they aren't by default)
 *
 * If any one of the predicates returns true for a class, then ReflectiveHierarchyStep uses that true value
 */
public final class ReflectiveClassFinalFieldsWritablePredicateBuildItem extends MultiBuildItem {

    private final Predicate<ClassInfo> predicate;

    public ReflectiveClassFinalFieldsWritablePredicateBuildItem(Predicate<ClassInfo> predicate) {
        this.predicate = predicate;
    }

    public Predicate<ClassInfo> getPredicate() {
        return predicate;
    }
}
