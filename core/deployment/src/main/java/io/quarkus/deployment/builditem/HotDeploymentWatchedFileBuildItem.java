package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A file that if modified may result in a hot redeployment when in the dev mode.
 */
public final class HotDeploymentWatchedFileBuildItem extends MultiBuildItem {

    private final String location;

    private final boolean restartNeeded;

    public HotDeploymentWatchedFileBuildItem(String location) {
        this(location, true);
    }

    public HotDeploymentWatchedFileBuildItem(String location, boolean restartNeeded) {
        this.location = location;
        this.restartNeeded = restartNeeded;
    }

    public String getLocation() {
        return location;
    }

    /**
     * 
     * @return {@code true} if a file change should result in an application restart, {@code false} otherwise
     */
    public boolean isRestartNeeded() {
        return restartNeeded;
    }

}
