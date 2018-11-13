package org.jboss.shamrock.deployment.cdi;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.DotName;

public final class ResourceAnnotationBuildItem extends MultiBuildItem {

    private final DotName name;

    public ResourceAnnotationBuildItem(DotName name) {
        this.name = name;
    }

    public DotName getName() {
        return name;
    }
}
