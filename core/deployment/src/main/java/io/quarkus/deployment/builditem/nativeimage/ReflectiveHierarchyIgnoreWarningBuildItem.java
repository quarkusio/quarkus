package io.quarkus.deployment.builditem.nativeimage;

import java.util.Objects;
import java.util.function.Predicate;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used by {@link io.quarkus.deployment.steps.ReflectiveHierarchyStep} to ignore reflection warning deliberately
 */
public final class ReflectiveHierarchyIgnoreWarningBuildItem extends MultiBuildItem {

    private final Predicate<DotName> predicate;

    // used by external extensions
    public ReflectiveHierarchyIgnoreWarningBuildItem(DotName dotName) {
        this.predicate = new DotNameExclusion(dotName);
    }

    public ReflectiveHierarchyIgnoreWarningBuildItem(Predicate<DotName> predicate) {
        this.predicate = predicate;
    }

    public Predicate<DotName> getPredicate() {
        return predicate;
    }

    public static class DotNameExclusion implements Predicate<DotName> {

        private final DotName dotName;

        public DotNameExclusion(DotName dotName) {
            this.dotName = Objects.requireNonNull(dotName);
        }

        @Override
        public boolean test(DotName input) {
            return input.equals(dotName);
        }
    }

}
