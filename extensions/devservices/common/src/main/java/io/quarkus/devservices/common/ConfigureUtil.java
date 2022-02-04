package io.quarkus.devservices.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.Base58;

public final class ConfigureUtil {

    private static final Map<String, Properties> DEVSERVICES_PROPS = new ConcurrentHashMap<>();

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

        String hostName = hostNamePrefix + "-" + Base58.randomString(5);
        container.setNetworkAliases(Collections.singletonList(hostName));

        return hostName;
    }

    public static String getDefaultImageNameFor(String devserviceName) {
        var imageName = DEVSERVICES_PROPS.computeIfAbsent(devserviceName, ConfigureUtil::loadProperties)
                .getProperty("default.image");
        if (imageName == null) {
            throw new IllegalArgumentException("No default.image configured for " + devserviceName);
        }
        return imageName;
    }

    private static Properties loadProperties(String devserviceName) {
        var fileName = devserviceName + "-devservice.properties";
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            if (in == null) {
                throw new IllegalArgumentException(fileName + " not found on classpath");
            }
            var properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
