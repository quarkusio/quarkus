package io.quarkus.deployment.builditem;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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

/**
 * This is a wrapper around the RunningDevServicesRegistry, so the registry can be loaded with the system classloader
 * The QuarkusClassLoader takes care of loading the tracker with the right classloader
 * <p>
 * This build item is used to manage the lifecycle of dev services across different features and launch modes.
 */
public final class DevServicesRegistryBuildItem extends SimpleBuildItem {

    private static final Logger log = Logger.getLogger(DevServicesRegistryBuildItem.class);

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

    public void startAll(Collection<DevServicesResultBuildItem> services,
            List<DevServicesCustomizerBuildItem> customizers,
            ClassLoader augmentClassLoader) {
        CompletableFuture.allOf(services.stream()
                .filter(DevServicesResultBuildItem::isStartable)
                .map(serv -> CompletableFuture.runAsync(() -> {
                    // We need to set the context classloader to the augment classloader, so that the dev services can be started with the right classloader
                    if (augmentClassLoader != null) {
                        Thread.currentThread().setContextClassLoader(augmentClassLoader);
                    } else {
                        Thread.currentThread().setContextClassLoader(serv.getClass().getClassLoader());
                    }
                    this.start(serv, customizers);
                }))
                .toArray(CompletableFuture[]::new)).join();
    }

    public void start(DevServicesResultBuildItem request, List<DevServicesCustomizerBuildItem> customizers) {
        // RunningService class is loaded on parent classloader
        RunningService matchedDevService = this.getRunningServices(request.getName(), request.getServiceName(),
                request.getServiceConfig());
        if (matchedDevService == null) {
            // There isn't a running container that has the right config, we need to do work
            // Let's get all the running dev services associated with this feature (+ launch mode plus named section), so we can close them
            closeAllRunningServices(request.getName(), request.getServiceName());

            reallyStart(request, customizers);
        }
    }

    private void reallyStart(DevServicesResultBuildItem request, List<DevServicesCustomizerBuildItem> customizers) {
        StartupLogCompressor compressor = new StartupLogCompressor("Dev Services Startup", null, null);
        try {
            Supplier<Startable> startableSupplier = request.getStartableSupplier();
            if (startableSupplier == null) {
                throw new IllegalStateException(
                        "Dev services for " + request.getName() + " requires a startable supplier, but none was provided.");
            }
            Startable startable = startableSupplier.get();
            for (DevServicesCustomizerBuildItem customizer : customizers) {
                startable = customizer.apply(request, startable);
            }
            startable.start();

            RunningService service = new RunningService(request.getName(), request.getDescription(),
                    request.getConfig(startable), startable.getContainerId(), startable);
            this.addRunningService(request.getName(), request.getServiceName(), request.getServiceConfig(), service);

            compressor.close();

            Consumer<Startable> postStartAction = request.getPostStartAction();
            if (postStartAction != null) {
                try {
                    postStartAction.accept(startable);
                } catch (Throwable t) {
                    log.errorf(t, "An error occurred while executing the post-start action for %s dev service: %s",
                            request.getName(), t.getMessage());
                }
            } else {
                log.infof("The %s dev service is ready to accept connections on %s", request.getName(),
                        startable.getConnectionInfo());
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw t;
        }
    }

}
