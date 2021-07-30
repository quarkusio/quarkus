package io.quarkus.devservices.common;

import java.util.Collections;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.Base58;

public final class ConfigureUtil {

    private ConfigureUtil() {
    }

    public static String configureSharedNetwork(GenericContainer<?> container, String hostNamePrefix) {
        // When a shared network is requested for the launched containers, we need to configure
        // the container to use it. We also need to create a hostname that will be applied to the returned
        // URL
        container.setNetwork(Network.SHARED);
        String hostName = hostNamePrefix + "-" + Base58.randomString(5);
        container.setNetworkAliases(Collections.singletonList(hostName));

        // we need to clear the exposed ports as they don't make sense when the application is going to
        // to be communicating with the DB over the same network
        container.setExposedPorts(Collections.emptyList());
        return hostName;
    }
}
