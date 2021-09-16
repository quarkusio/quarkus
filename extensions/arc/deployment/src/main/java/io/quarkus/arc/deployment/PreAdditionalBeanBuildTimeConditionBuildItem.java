package io.quarkus.arc.deployment;

import org.jboss.jandex.AnnotationTarget;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A type of build item that is similar to {@link BuildTimeConditionBuildItem} but evaluated before
 * processing the {@link AdditionalBeanBuildItem} in order to filter the beans thanks to build time conditions
 * before actually adding them with a {@link AdditionalBeanBuildItem}.
 *
 * @see io.quarkus.arc.deployment.BuildTimeConditionBuildItem
 * @see io.quarkus.arc.deployment.AdditionalBeanBuildItem
 * @deprecated This build item is no longer needed and will be removed at some point post Quarkus 2.3. The
 *             {@link BuildTimeConditionBuildItem} can be used instead.
 */
@Deprecated
public final class PreAdditionalBeanBuildTimeConditionBuildItem extends MultiBuildItem {

    private final AnnotationTarget target;
    private final boolean enabled;

    public PreAdditionalBeanBuildTimeConditionBuildItem(AnnotationTarget target, boolean enabled) {
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
