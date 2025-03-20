package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The network id of the network that the dev services are running on.
 */
public final class DevServicesNetworkIdBuildItem extends SimpleBuildItem {

    private final String networkId;

    public DevServicesNetworkIdBuildItem(String networkId) {
        this.networkId = networkId;
    }

    public String getNetworkId() {
        return networkId;
    }
}
