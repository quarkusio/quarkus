package io.quarkus.deployment.builditem;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.crossclassloader.runtime.ComparableDevServicesConfig;
import io.quarkus.devservices.crossclassloader.runtime.DevServiceOwner;
import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesRegistry;
import io.quarkus.devservices.crossclassloader.runtime.RunningService;
import io.quarkus.runtime.LaunchMode;

// Ideally we would have a unique build item for each processor/feature, but that would need a new KeyedBuildItem or FeatureBuildItem type
// Needs to be in core because DevServicesResultBuildItem is in core
public final class DevServicesRegistryBuildItem extends SimpleBuildItem {

    private static final Logger log = Logger.getLogger(DevServicesRegistryBuildItem.class);

    // This is a fairly thin wrapper around the tracker, so the tracker can be loaded with the system classloader
    // The QuarkusClassLoader takes care of loading the tracker with the right classloader
    private final UUID uuid;
    private final DevServicesConfig globalConfig;
    private final LaunchMode launchMode;

    public DevServicesRegistryBuildItem(UUID uuid, DevServicesConfig globalDevServicesConfig, LaunchMode launchMode) {
        this.launchMode = launchMode;
        this.uuid = uuid;
        this.globalConfig = globalDevServicesConfig;
    }

    public Set<RunningService> getRunningServices(String featureName, String configName, Object identifyingConfig) {
        DevServiceOwner owner = new DevServiceOwner(featureName, launchMode.name(), configName);
        ComparableDevServicesConfig key = new ComparableDevServicesConfig(uuid, owner, globalConfig, identifyingConfig);
        return RunningDevServicesRegistry.INSTANCE.getRunningServices(key);
    }

    public Set<RunningService> getAllRunningServices(String featureName, String configName) {
        DevServiceOwner owner = new DevServiceOwner(featureName, launchMode.name(), configName);
        return RunningDevServicesRegistry.INSTANCE.getAllRunningServices(owner);
    }

    public Set<RunningService> getAllRunningServices(String launchMode) {
        return RunningDevServicesRegistry.INSTANCE.getAllRunningServices(launchMode);
    }

    public void addRunningService(String featureName, String configName, Object identifyingConfig,
            RunningService service) {
        DevServiceOwner owner = new DevServiceOwner(featureName, launchMode.name(), configName);
        ComparableDevServicesConfig key = new ComparableDevServicesConfig(uuid, owner, globalConfig, identifyingConfig);
        RunningDevServicesRegistry.INSTANCE.addRunningService(key, service);
    }

    public void removeRunningService(String featureName, String configName, Object identifyingConfig, RunningService service) {
        DevServiceOwner owner = new DevServiceOwner(featureName, launchMode.name(), configName);
        ComparableDevServicesConfig key = new ComparableDevServicesConfig(uuid, owner, globalConfig, identifyingConfig);
        RunningDevServicesRegistry.INSTANCE.removeRunningService(key, service);
    }

    public void closeAllRunningServices() {
        RunningDevServicesRegistry.INSTANCE.closeAllRunningServices(launchMode.name());
    }

    public Map<String, String> getConfigForAllRunningServices() {
        Map<String, String> config = new HashMap<>();
        for (Supplier<Map<String, String>> configProvider : RunningDevServicesRegistry.INSTANCE
                .getConfigForAllRunningServices(launchMode.name())) {
            config.putAll(configProvider.get());
        }
        return config;
    }

    public void start(DevServicesRequestBuildItem request) {
        StartupLogCompressor compressor = new StartupLogCompressor("Dev Services Startup", null, null);
        try {
            // These RunnableDevService classes could be from another classloader, so don't make assumptions about the class
            Collection<RunningService> matchedDevServices = this.getRunningServices(request.featureName, request.serviceName,
                    request.serviceConfig);
            // if the redis containers have already started we just return; if we wanted to be very cautious we could check the entries for an isRunningStatus, but they might be in the wrong classloader, so that's hard work
            if (matchedDevServices == null || matchedDevServices.isEmpty()) {
                // There isn't a running container that has the right config, we need to do work
                // Let's get all the running dev services associated with this feature (+ launch mode plus named section), so we can close them
                closeOwnedServices(request.featureName, request.serviceName);

                reallyStart(request);
            }

            compressor.close();
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw t;
        }

    }

    private void closeOwnedServices(String featureName, String configName) {
        Collection<RunningService> unusableDevServices = this.getAllRunningServices(featureName, configName);
        if (unusableDevServices != null) {
            for (RunningService service : unusableDevServices) {
                try {
                    service.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void reallyStart(DevServicesRequestBuildItem request) {
        Startable container = request.startableSupplier.get();
        container.start();

        RunningService service = new RunningService(request.featureName, request.featureName + " - " + request.serviceName,
                request.getConfig(container), container.getContainerId(), self -> {
                    try {
                        DevServicesRegistryBuildItem.this.removeRunningService(request.featureName, request.serviceName,
                                request.serviceConfig, self);
                        container.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        this.addRunningService(request.featureName, request.serviceName, request.serviceConfig, service);
        // Ideally we'd print out a port number here, but we can only do that if we add a dependency on GenericContainer (or update startable to add a method)

        log.infof("The %s dev service is ready to accept connections on %s", request.featureName,
                container.getConnectionInfo());
    }

}
