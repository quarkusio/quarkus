package io.quarkus.websockets.client.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item indicating that a
 * {@link io.undertow.websockets.WebSocketDeploymentInfo} service has been
 * registered for static-init.
 */
public final class WebSocketDeploymentInfoBuildItem extends SimpleBuildItem {
}
