package io.quarkus.resteasy.common.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Used to propagate the configuration of the RESTEasy server.
 */
public final class ResteasyConfigBuildItem extends SimpleBuildItem {

    private boolean jsonDefault;

    public ResteasyConfigBuildItem(boolean jsonDefault) {
        this.jsonDefault = jsonDefault;
    }

    public boolean isJsonDefault() {
        return jsonDefault;
    }
}
