package io.quarkus.observability.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class DevResourceEnabledBuildItem extends MultiBuildItem {

    private final String devId;

    public DevResourceEnabledBuildItem(String devId) {
        this.devId = devId;
    }

    public String getDevId() {
        return devId;
    }
}
