package io.quarkus.devservices.common;

import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;
import static io.quarkus.devservices.common.Labels.QUARKUS_LAUNCH_MODE;
import static io.quarkus.devservices.common.Labels.QUARKUS_PROCESS_UUID;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.Base58;

import com.github.dockerjava.api.command.CreateNetworkCmd;

import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesRegistry;
import io.quarkus.runtime.LaunchMode;

public final class ConfigureUtil {

    private static final Map<String, Properties> DEVSERVICES_PROPS = new ConcurrentHashMap<>();

    private ConfigureUtil() {
    }

    public static String configureNetwork(GenericContainer<?> container,
            String defaultNetworkId,
            boolean useSharedNetwork,
            String hostNamePrefix) {
        if (defaultNetworkId != null) {
            // Set the network`without creating the network
            container.setNetworkMode(defaultNetworkId);
            return setGeneratedHostname(container, hostNamePrefix);
        } else if (useSharedNetwork) {
            return configureSharedNetwork(container, hostNamePrefix);
        }
        return container.getHost();
    }

    public static void configureLabels(GenericContainer<?> container, LaunchMode launchMode, String serviceLabel,
            String serviceName) {
        if (serviceName != null) {
            container.withLabel(serviceLabel, serviceName);
            container.withLabel(QUARKUS_DEV_SERVICE, serviceName);
        }
        configureLabels(container, launchMode);
    }

    public static void configureLabels(GenericContainer<?> container, LaunchMode launchMode) {
        // Configure the labels for the container
        container.withLabel(QUARKUS_PROCESS_UUID, RunningDevServicesRegistry.APPLICATION_UUID);
        container.withLabel(QUARKUS_LAUNCH_MODE, launchMode.toString());
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
                Consumer<CreateNetworkCmd> addDevservicesLabel = cmd -> cmd
                        .withLabels(Map.of("quarkus.devservices.network", "shared"));
                Field createNetworkCmdModifiersField = sharedNetwork.getClass().getSuperclass()
                        .getDeclaredField("createNetworkCmdModifiers");
                createNetworkCmdModifiersField.setAccessible(true);
                createNetworkCmdModifiersField.set(sharedNetwork, Set.of(addDevservicesLabel));
                container.setNetwork((Network) sharedNetwork);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to obtain SHARED network from testcontainers", e);
            }
        } else {
            container.setNetwork(Network.SHARED);
        }
        return setGeneratedHostname(container, hostNamePrefix);
    }

    public static String setGeneratedHostname(GenericContainer<?> container, String hostNamePrefix) {
        String hostName = (hostNamePrefix + "-" + Base58.randomString(5)).toLowerCase(Locale.ROOT);
        // some containers might try to add their own aliases on start, so we want to keep this list modifiable:
        container.setNetworkAliases(new ArrayList<>(List.of(hostName)));

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
