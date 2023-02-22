package io.quarkus.devui.deployment;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;

/**
 * All the routes needed for Dev UI
 */
public final class DevUIRoutesBuildItem extends MultiBuildItem {

    private final String path;
    private final String finalDestination;
    private final List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations;

    public DevUIRoutesBuildItem(String path, String finalDestination,
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations) {
        this.path = path;
        this.finalDestination = finalDestination;
        this.webRootConfigurations = webRootConfigurations;
    }

    public String getPath() {
        return path;
    }

    public String getFinalDestination() {
        return finalDestination;
    }

    public List<FileSystemStaticHandler.StaticWebRootConfiguration> getWebRootConfigurations() {
        return webRootConfigurations;
    }
}
