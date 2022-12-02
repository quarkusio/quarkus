package io.quarkus.arc.deployment;

import org.jboss.jandex.AnnotationTarget;

import io.quarkus.builder.item.MultiBuildItem;

public final class BuildTimeConditionBuildItem extends MultiBuildItem {

    private final AnnotationTarget target;
    private final boolean enabled;

    public BuildTimeConditionBuildItem(AnnotationTarget target, boolean enabled) {
        switch (target.kind()) {
            case CLASS:
            case METHOD:
            case FIELD:
                this.target = target;
                break;
            default:
                throw new IllegalArgumentException("'target' can only be a class, a field or a method");
        }
        this.enabled = enabled;
    }

    public AnnotationTarget getTarget() {
        return target;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
