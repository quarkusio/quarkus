package io.quarkus.arc.deployment;

import org.jboss.jandex.AnnotationTarget;

import io.quarkus.builder.item.MultiBuildItem;

public final class BuildTimeConditionBuildItem extends MultiBuildItem {

    private final AnnotationTarget target;
    private final boolean enabled;

    public BuildTimeConditionBuildItem(AnnotationTarget target, boolean enabled) {
        AnnotationTarget.Kind kind = target.kind();
        if ((kind != AnnotationTarget.Kind.CLASS) && (kind != AnnotationTarget.Kind.METHOD)
                && (kind != AnnotationTarget.Kind.FIELD)) {
            throw new IllegalArgumentException("'target' can only be a class, a field or a method");
        }
        this.target = target;
        this.enabled = enabled;
    }

    public AnnotationTarget getTarget() {
        return target;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
