package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;

public final class ResourceBuildItem extends MultiBuildItem {

    private final String name;

    public ResourceBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
