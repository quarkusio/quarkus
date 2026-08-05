package io.quarkus.amazon.lambda.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Allows extensions to override the default mock event server port.
 * Used by amazon-lambda-http and amazon-lambda-rest to avoid port conflicts
 * when the real Vert.x HTTP server also starts in dev mode.
 */
public final class EventServerPortOverrideBuildItem extends SimpleBuildItem {

    private final int port;

    public EventServerPortOverrideBuildItem(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
