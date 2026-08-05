package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The network id of the network that the dev services are running on.
 * This is intended to be consumed in the IntegrationTest launcher to ensure that the application is running on the same network
 * as the dev services.
 * <p>
 * In the future if extensions consume this build item, it would create the shared network if it doesn't exist,
 * and use it for the dev services and the test containers.
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
