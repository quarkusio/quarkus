package io.quarkus.deployment.builditem;

import java.util.HashMap;
import java.util.Map;
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

    public RunningService getRunningServices(String featureName, String configName, Object identifyingConfig) {
        DevServiceOwner owner = new DevServiceOwner(featureName, launchMode.name(), configName);
        ComparableDevServicesConfig key = new ComparableDevServicesConfig(uuid, owner, globalConfig, identifyingConfig);
        return RunningDevServicesRegistry.INSTANCE.getRunningServices(key);
    }

    public void addRunningService(String featureName, String configName, Object identifyingConfig,
            RunningService service) {
        DevServiceOwner owner = new DevServiceOwner(featureName, launchMode.name(), configName);
        ComparableDevServicesConfig key = new ComparableDevServicesConfig(uuid, owner, globalConfig, identifyingConfig);
        RunningDevServicesRegistry.INSTANCE.addRunningService(key, service);
    }

    public void closeAllRunningServices(String featureName, String configName) {
        DevServiceOwner owner = new DevServiceOwner(featureName, launchMode.name(), configName);
        RunningDevServicesRegistry.INSTANCE.closeAllRunningServices(owner);
    }

    public void closeAllRunningServices() {
        RunningDevServicesRegistry.INSTANCE.closeAllRunningServices(launchMode.name());
    }

    public Map<String, String> getConfigForAllRunningServices() {
        Map<String, String> config = new HashMap<>();
        for (RunningService service : RunningDevServicesRegistry.INSTANCE.getAllRunningServices(launchMode.name())) {
            config.putAll(service.configs());
        }
        return config;
    }

    public void start(DevServicesResultBuildItem request) {
        StartupLogCompressor compressor = new StartupLogCompressor("Dev Services Startup", null, null);
        try {
            // These RunnableDevService classes could be from another classloader, so don't make assumptions about the class
            RunningService matchedDevService = this.getRunningServices(request.getName(), request.getServiceName(),
                    request.getServiceConfig());
            // if the redis containers have already started we just return; if we wanted to be very cautious we could check the entries for an isRunningStatus, but they might be in the wrong classloader, so that's hard work
            if (matchedDevService == null) {
                // There isn't a running container that has the right config, we need to do work
                // Let's get all the running dev services associated with this feature (+ launch mode plus named section), so we can close them
                closeAllRunningServices(request.getName(), request.getServiceName());

                reallyStart(request);
            }

            compressor.close();
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw t;
        }

    }

    private void reallyStart(DevServicesResultBuildItem request) {
        Supplier<Startable> startableSupplier = request.getStartableSupplier();
        if (startableSupplier == null) {
            throw new IllegalStateException(
                    "Dev services for " + request.getName() + " requires a startable supplier, but none was provided.");
        }
        Startable container = startableSupplier.get();
        container.start();

        RunningService service = new RunningService(request.getName(), request.getDescription(),
                request.getConfig(container), container.getContainerId(), container);
        this.addRunningService(request.getName(), request.getServiceName(), request.getServiceConfig(), service);
        // Ideally we'd print out a port number here, but we can only do that if we add a dependency on GenericContainer (or update startable to add a method)

        log.infof("The %s dev service is ready to accept connections on %s", request.getName(),
                container.getConnectionInfo());
    }

}
