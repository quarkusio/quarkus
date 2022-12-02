package io.quarkus.deployment.builditem;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item that contains the final results of all
 */
public final class DevServicesLauncherConfigResultBuildItem extends SimpleBuildItem {

    final Map<String, String> config;

    public DevServicesLauncherConfigResultBuildItem(Map<String, String> config) {
        this.config = config;
    }

    public Map<String, String> getConfig() {
        return config;
    }
}
