package io.quarkus.devservices.common;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesNetworkIdBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;

/**
 * Reports the network the Dev Services containers are running on.
 * <p>
 * This lives next to {@link ConfigureUtil#configureSharedNetwork}, which is what actually joins those containers to the
 * shared network. Both halves have to be on the deployment classpath together: an extension that has the joiner but not
 * the reporter starts its containers on the shared network while the integration test launcher, seeing no network id,
 * launches the application container on a different network of its own.
 */
public class DevServicesNetworkProcessor {

    private static final Logger log = Logger.getLogger(DevServicesNetworkProcessor.class);

    @BuildStep
    public DevServicesNetworkIdBuildItem networkId(
            DevServicesConfig devServicesConfig,
            List<DevServicesSharedNetworkBuildItem> sharedNetworkBuildItems,
            Optional<DevServicesComposeProjectBuildItem> composeProjectBuildItem) {
        Optional<String> configuredNetwork = ConfigProvider.getConfig().getOptionalValue(
                "quarkus.test.container.network", String.class);
        String networkId = configuredNetwork.flatMap(this::getOrCreateNetworkId)
                .or(() -> composeProjectBuildItem.map(DevServicesComposeProjectBuildItem::getDefaultNetworkId))
                .orElseGet(() -> (devServicesConfig.launchOnSharedNetwork() || !sharedNetworkBuildItems.isEmpty())
                        ? getSharedNetworkId()
                        : null);
        return new DevServicesNetworkIdBuildItem(networkId);
    }

    private Optional<String> getOrCreateNetworkId(String name) {
        // Skip creation for pre-defined/reserved network names
        if ("default".equals(name) || // alias to choose the platform-specific default network stack
                "host".equals(name) || // NetworkMode host is selected
                "none".equals(name) || // NetworkMode none is selected
                "bridge".equals(name) || // NetworkMode bridge is selected (default network on Linux)
                "nat".equals(name) || // NetworkMode nat is selected (default network on Windows)
                "container".equals(name) || // NetworkMode container is selected
                name.startsWith("container:") // NetworkMode container is selected with a specific container name or id
        ) {
            return Optional.of(name);
        }
        var networks = DockerClientFactory.lazyClient().listNetworksCmd().exec();
        for (var network : networks) {
            if (network.getName().equals(name)) {
                return Optional.of(network.getId());
            }
        }
        // if the network doesn't exist, create it
        try {
            // do the cleanup in a shutdown hook because there might be more services (launched via QuarkusTestResourceLifecycleManager) connected to the network
            String id = DockerClientFactory.lazyClient().createNetworkCmd().withName(name).exec().getId();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DockerClientFactory.lazyClient().removeNetworkCmd(id).exec();
                    } catch (Exception e) {
                        log.errorf("Unable to delete container network '%s'", id);
                    }
                }
            }));
            return Optional.of(id);
        } catch (Exception e) {
            log.warnf(e, "Creating container network '%s' completed unsuccessfully", name);
            return Optional.empty();
        }
    }

    /**
     * Get the network id from the shared testcontainers network, Creates the SHARED Network instance if not already created
     *
     * @return the network id if available, null otherwise
     */
    private String getSharedNetworkId() {
        try {
            Method id;
            Object sharedNetwork;
            var tccl = Thread.currentThread().getContextClassLoader();
            if (tccl.getName().contains("Deployment")) {
                Class<?> networkClass = tccl.getParent().loadClass("org.testcontainers.containers.Network");
                sharedNetwork = networkClass.getField("SHARED").get(null);
                Class<?> networkImplClass = tccl.getParent().loadClass("org.testcontainers.containers.Network$NetworkImpl");
                id = networkImplClass.getDeclaredMethod("getId");
            } else {
                sharedNetwork = Network.SHARED;
                id = Network.NetworkImpl.class.getDeclaredMethod("getId");
            }
            return (String) id.invoke(sharedNetwork);
        } catch (Exception e) {
            return null;
        }
    }
}
