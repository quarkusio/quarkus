package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A symbolic class that represents that an initialization task has been complete.
 * Similar to {@link ServiceStartBuildItem} but focused on initialization tasks (e.g. db migrations etc) that are run during
 * runtime just before the application startups.
 * <p>
 * The build item is used, so that we can track when all intialization tasks have been completed.
 */
public final class InitalizationTaskCompletedBuildItem extends MultiBuildItem {

    private final String name;

    public InitalizationTaskCompletedBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
