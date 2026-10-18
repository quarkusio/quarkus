package io.quarkus.security.jpa.common.deployment;

import java.util.Set;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item to evaluate whether the class is a Panache model class.
 */
public final class PanacheEntityPredicateBuildItem extends SimpleBuildItem {

    private final Set<String> modelClasses;

    public PanacheEntityPredicateBuildItem(Set<String> modelClasses) {
        this.modelClasses = Set.copyOf(modelClasses);
    }

    public boolean isPanache(ClassInfo annotatedClass) {
        return modelClasses.contains(annotatedClass.name().toString());
    }

}
