package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;

/**
 * A symbolic class that represents a service start.
 */
public final class ServiceStartBuildItem extends MultiBuildItem {

    private final String name;

    public ServiceStartBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
