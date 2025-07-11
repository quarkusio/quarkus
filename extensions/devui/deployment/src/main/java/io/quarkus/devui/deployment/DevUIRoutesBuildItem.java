package io.quarkus.devui.deployment;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;

/**
 * All the routes needed for Dev UI
 */
public final class DevUIRoutesBuildItem extends MultiBuildItem {

    private final String namespace;
    private final String contextRoot;
    private final String finalDestination;
    private final List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations;

    public DevUIRoutesBuildItem(String namespace, String contextRoot, String finalDestination,
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations) {
        this.namespace = namespace;
        this.contextRoot = contextRoot;
        this.finalDestination = finalDestination;
        this.webRootConfigurations = webRootConfigurations;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public String getFinalDestination() {
        return finalDestination;
    }

    public List<FileSystemStaticHandler.StaticWebRootConfiguration> getWebRootConfigurations() {
        return webRootConfigurations;
    }

    public String getPath() {
        return contextRoot + "/" + namespace;
    }
}
