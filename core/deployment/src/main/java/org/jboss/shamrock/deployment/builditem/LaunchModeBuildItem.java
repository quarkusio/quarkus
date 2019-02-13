package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.runtime.LaunchMode;

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
