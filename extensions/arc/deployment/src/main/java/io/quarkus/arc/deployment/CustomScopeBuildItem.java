package io.quarkus.arc.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * An extension that registers a custom CDI context via {@link ContextRegistrationPhaseBuildItem} should produce this build
 * item in order to contribute the custom scope annotation name to the set of bean defining annotations.
 * 
 * @see CustomScopeAnnotationsBuildItem
 * @see ContextRegistrationPhaseBuildItem
 */
public final class CustomScopeBuildItem extends MultiBuildItem {

    private final DotName annotationName;

    public CustomScopeBuildItem(DotName annotationName) {
        this.annotationName = annotationName;
    }

    public DotName getAnnotationName() {
        return annotationName;
    }

}
