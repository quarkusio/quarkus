package io.quarkus.resteasy.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that represents a JAX-RS config.
 */
public final class ResteasyJaxrsConfigBuildItem extends SimpleBuildItem {

    public final String defaultPath;

    public ResteasyJaxrsConfigBuildItem(String defaultPath) {
        this.defaultPath = defaultPath;
    }
}
