package io.quarkus.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;

import io.quarkus.runtime.LaunchMode;

/**
 * indicates the type of launch
 */
public final class LaunchModeBuildItem extends SimpleBuildItem {

    private final LaunchMode launchMode;

    public LaunchModeBuildItem(LaunchMode launchMode) {
        this.launchMode = launchMode;
    }

    public LaunchMode getLaunchMode() {
        return launchMode;
    }
}
