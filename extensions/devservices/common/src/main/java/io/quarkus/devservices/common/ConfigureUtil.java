package io.quarkus.devservices.common;

import java.util.Collections;
import java.util.Locale;

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

        var tccl = Thread.currentThread().getContextClassLoader();
        if (tccl.getName().contains("Deployment")) {
            // we need to use the shared network loaded from the Augmentation ClassLoader because that ClassLoader
            // is what the test launching process (that has access to the curated application) has access to
            // FIXME: This is an ugly hack, but there is not much we can do...
            try {
                Class<?> networkClass = tccl.getParent()
                        .loadClass("org.testcontainers.containers.Network");
                Object sharedNetwork = networkClass.getField("SHARED").get(null);
                container.setNetwork((Network) sharedNetwork);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to obtain SHARED network from testcontainers", e);
            }
        } else {
            container.setNetwork(Network.SHARED);
        }

        String hostName = (hostNamePrefix + "-" + Base58.randomString(5)).toLowerCase(Locale.ROOT);
        container.setNetworkAliases(Collections.singletonList(hostName));

        return hostName;
    }
}
