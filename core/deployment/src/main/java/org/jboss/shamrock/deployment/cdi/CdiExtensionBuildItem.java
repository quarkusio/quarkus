package org.jboss.shamrock.deployment.cdi;

import org.jboss.builder.item.MultiBuildItem;

public final class CdiExtensionBuildItem extends MultiBuildItem {

    private final String name;

    public CdiExtensionBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
