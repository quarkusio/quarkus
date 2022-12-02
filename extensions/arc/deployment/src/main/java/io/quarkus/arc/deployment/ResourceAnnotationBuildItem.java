package io.quarkus.arc.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item is used to specify resource annotations that makes it possible to resolve non-CDI injection points, such as
 * Java EE resources.
 */
public final class ResourceAnnotationBuildItem extends MultiBuildItem {

    private final DotName name;

    public ResourceAnnotationBuildItem(DotName name) {
        this.name = name;
    }

    public DotName getName() {
        return name;
    }
}
