package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.runtime.StartupEvent;

/**
 * A symbolic class that represents a service start.
 * <p>
 * {@link StartupEvent} is fired after all services are started.
 */
public final class ServiceStartBuildItem extends MultiBuildItem {

    private final String name;

    public ServiceStartBuildItem(Feature feature) {
        this(feature.getName());
    }

    public ServiceStartBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
