package io.quarkus.vertx.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item which indicates that the current Vertx request context data needs
 * to be copied into the connection context
 */
public final class CopyVertxContextDataBuildItem extends MultiBuildItem {

    private final String property;

    public CopyVertxContextDataBuildItem(String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }
}
