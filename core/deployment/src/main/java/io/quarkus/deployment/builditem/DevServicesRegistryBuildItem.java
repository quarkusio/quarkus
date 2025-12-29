package io.quarkus.deployment.builditem;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    public DevServicesRegistryBuildItem(UUID uuid, DevServicesConfig devServicesConfig, LaunchMode launchMode) {
        this.launchMode = launchMode;
        this.uuid = uuid;
        this.globalConfig = devServicesConfig;
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

    public void closeRemainingRunningServices(Collection<DevServicesResultBuildItem> services) {
        Set<DevServiceOwner> ownersToKeep = services.stream()
                .map(s -> new DevServiceOwner(s.getName(), launchMode.name(), s.getServiceName()))
                .collect(Collectors.toSet());
        RunningDevServicesRegistry.INSTANCE.closeRemainingRunningServices(uuid, launchMode.name(), ownersToKeep);
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
            List<DevServicesAdditionalConfigBuildItem> additionalConfigBuildItems,
            ClassLoader augmentClassLoader) {
        closeRemainingRunningServices(services);
        Map<String, String> config = new ConcurrentHashMap<>();
        startSelectedServices(services, customizers, additionalConfigBuildItems, augmentClassLoader,
                dr -> !dr.hasDependencies(), config);

        // Now start everything with a dependency
        // This won't handle the case where the dependencies also have dependencies, but that can be a follow-on work item if people ask for it
        // I think we could implement it by getting the actual dependencies and seeing if any of them are also in the list of things we're starting, and then recursing
        startSelectedServices(services, customizers, additionalConfigBuildItems, augmentClassLoader,
                DevServicesResultBuildItem::hasDependencies, config);

    }

    private void startSelectedServices(Collection<DevServicesResultBuildItem> services,
            List<DevServicesCustomizerBuildItem> customizers,
            List<DevServicesAdditionalConfigBuildItem> additionalConfigBuildItems,
            ClassLoader augmentClassLoader, Predicate<? super DevServicesResultBuildItem> filter, Map<String, String> config) {
        // TODO Note that this does not handle chained dependencies; dependencies can only be one level deep for now
        // It would be easy to fix that, but let's wait until we need to
        CompletableFuture.allOf(services.stream()
                .filter(DevServicesResultBuildItem::isStartable)
                .filter(filter)
                .map(serv -> CompletableFuture.runAsync(() -> {
                    // We need to set the context classloader to the augment classloader, so that the dev services can be started with the right classloader
                    if (augmentClassLoader != null) {
                        Thread.currentThread().setContextClassLoader(augmentClassLoader);
                    } else {
                        Thread.currentThread().setContextClassLoader(serv.getClass().getClassLoader());
                    }
                    this.start(serv, customizers, additionalConfigBuildItems, config);
                }))
                .toArray(CompletableFuture[]::new)).join();
    }

    public void start(DevServicesResultBuildItem request, List<DevServicesCustomizerBuildItem> customizers,
            List<DevServicesAdditionalConfigBuildItem> additionalConfigBuildItems, Map<String, String> config) {
        // RunningService class is loaded on parent classloader
        RunningService matchedDevService = this.getRunningServices(request.getName(), request.getServiceName(),
                request.getServiceConfig());

        if (matchedDevService == null) {
            // There isn't a running container that has the right config, we need to do work
            // Let's get all the running dev services associated with this feature (+ launch mode plus named section), so we can close them
            closeAllRunningServices(request.getName(), request.getServiceName());

            reallyStart(request, customizers, additionalConfigBuildItems, config);
        }
    }

    private void reallyStart(DevServicesResultBuildItem request, List<DevServicesCustomizerBuildItem> customizers,
            List<DevServicesAdditionalConfigBuildItem> additionalConfigBuildItems, Map<String, String> configs) {
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

            String missingDependency = null;

            // The config from the new sources isn't easily available via ConfigProvider.getConfig(), so directly inject it into services which depend on it
            var dependencies = request.getDependencies();
            if (dependencies != null && !dependencies.isEmpty()) {
                for (DevServicesResultBuildItem.DevServiceConfigDependency<? extends Startable> dependency : dependencies) {

                    var value = configs.get(dependency.requiredConfigKey());
                    if (value != null) {
                        ((BiConsumer<Startable, String>) dependency.valueInjector()).accept(startable, value);
                    } else {
                        missingDependency = dependency.requiredConfigKey();
                    }
                }
            }

            var optionalDependencies = request.getOptionalDependencies();
            if (optionalDependencies != null && !optionalDependencies.isEmpty()) {
                for (DevServicesResultBuildItem.DevServiceConfigDependency<? extends Startable> dependency : optionalDependencies) {
                    var value = configs.get(dependency.requiredConfigKey());
                    if (value != null) {
                        ((BiConsumer<Startable, String>) dependency.valueInjector()).accept(startable, value);
                    }
                }
            }

            if (missingDependency == null) {
                startable.start();
                // We do not "copy" the config map here since it is created within the request.getConfig:
                Map<String, String> combinedConfig = request.getConfig(startable);
                // Some extensions may rely on adding/overriding config properties
                //  depending on the results of the started dev services,
                //  e.g. Hibernate Search/ORM may change the default schema management
                //  if it detects that it runs over a dev service datasource/Elasticsearch distribution.
                for (DevServicesAdditionalConfigBuildItem additionalConfigBuildItem : additionalConfigBuildItems) {
                    Map<String, String> extraFromBuildItem = additionalConfigBuildItem.getConfigProvider()
                            .provide(combinedConfig);
                    if (!extraFromBuildItem.isEmpty()) {
                        combinedConfig.putAll(extraFromBuildItem);
                    }
                }
                RunningService service = new RunningService(request.getName(), request.getDescription(),
                        combinedConfig, request.getOverrideConfig(startable), startable.getContainerId(), startable);
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
            } else {
                log.infof("The %s dev service did not start because the configuration with key %s did not become available",
                        request.getName(),
                        missingDependency);
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw t;
        }
    }

}
