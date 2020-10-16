package io.quarkus.resteasy.common.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ResteasyConfigBuildItem extends SimpleBuildItem {

    private boolean jsonDefault;

    public ResteasyConfigBuildItem(boolean jsonDefault) {
        this.jsonDefault = jsonDefault;
    }

    public boolean isJsonDefault() {
        return jsonDefault;
    }
}
