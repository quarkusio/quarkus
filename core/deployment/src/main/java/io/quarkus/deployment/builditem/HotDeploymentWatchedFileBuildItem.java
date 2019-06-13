package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A file that if modified should result in a hot redeployment when in
 * dev mode.
 */
public final class HotDeploymentWatchedFileBuildItem extends MultiBuildItem {

    private final String location;

    public HotDeploymentWatchedFileBuildItem(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }
}
