package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A symbolic class that represents that an initialization task has been completed.
 * Similar to {@link ServiceStartBuildItem} but focused on initialization tasks (e.g. db migrations etc) that are run during
 * runtime just before the application starts.
 * <p>
 * The build item is used, so that we can track when all initialization tasks have been completed.
 */
public final class InitTaskCompletedBuildItem extends MultiBuildItem {

    private final String name;

    public InitTaskCompletedBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
