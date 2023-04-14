package io.quarkus.observability.deployment;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.common.ContainerShutdownCloseable;
import io.quarkus.observability.common.config.ContainerConfig;
import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.observability.devresource.DevResourceLifecycleManager;
import io.quarkus.observability.devresource.DevResources;
import io.quarkus.observability.runtime.config.ObservabilityConfiguration;
import io.quarkus.runtime.LaunchMode;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { GlobalDevServicesConfig.Enabled.class,
        ObservabilityProcessor.IsEnabled.class })
class ObservabilityProcessor {
    private static final Logger log = Logger.getLogger(ObservabilityProcessor.class);

    private static final Map<String, DevServicesResultBuildItem.RunningDevService> devServices = new ConcurrentHashMap<>();
    private static final Map<String, ContainerConfig> capturedDevServicesConfigurations = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> firstStart = new ConcurrentHashMap<>();

    public static class IsEnabled implements BooleanSupplier {
        ObservabilityConfiguration config;

        public boolean getAsBoolean() {
            return config.enabled() && !config.devResources();
        }
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.OBSERVABILITY);
    }

    private String devId(DevResourceLifecycleManager<?> dev) {
        String sn = dev.getClass().getSimpleName();
        int p = sn.indexOf("Resource");
        return sn.substring(0, p != -1 ? p : sn.length());
    }

    @BuildStep
    public void startContainers(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            ObservabilityConfiguration configuration,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig devServicesConfig,
            BuildProducer<DevServicesResultBuildItem> services) {

        if (!configuration.enabled()) {
            log.infof("Observability dev services are disabled in config");
            return;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warn("Please get a working Docker instance");
            return;
        }

        @SuppressWarnings("rawtypes")
        List<DevResourceLifecycleManager> resources = DevResources.resources();
        // this should throw an exception on a duplicate
        //noinspection ResultOfMethodCallIgnored
        resources.stream().collect(Collectors.toMap(this::devId, Function.identity()));

        @SuppressWarnings("rawtypes")
        Stream<DevResourceLifecycleManager> stream = resources.stream();
        if (configuration.parallel()) {
            stream = stream.parallel();
        }

        stream.forEach(dev -> {
            String devId = devId(dev);

            DevServicesResultBuildItem.RunningDevService devService = devServices.remove(devId);
            ContainerConfig currentDevServicesConfiguration = dev.config(configuration);

            if (devService != null) {
                ContainerConfig capturedDevServicesConfiguration = capturedDevServicesConfigurations.remove(devId);
                boolean restartRequired = !currentDevServicesConfiguration.equals(capturedDevServicesConfiguration);
                if (!restartRequired) {
                    services.produce(devService.toBuildItem());
                    return;
                }
                try {
                    devService.close();
                } catch (Throwable e) {
                    log.errorf("Failed to stop %s container", devId, e);
                }
            }

            capturedDevServicesConfigurations.put(devId, currentDevServicesConfiguration);

            StartupLogCompressor compressor = new StartupLogCompressor(
                    (launchMode.isTest() ? "(test) " : "") + devId + " Dev Services Starting:",
                    consoleInstalledBuildItem,
                    loggingSetupBuildItem);
            try {
                DevServicesResultBuildItem.RunningDevService newDevService = startContainer(
                        devId,
                        dev,
                        currentDevServicesConfiguration,
                        configuration,
                        devServicesConfig.timeout);
                if (newDevService == null) {
                    compressor.closeAndDumpCaptured();
                    return;
                } else {
                    compressor.close();
                }

                devService = newDevService;
                devServices.put(devId, newDevService);
            } catch (Throwable t) {
                compressor.closeAndDumpCaptured();
                throw new RuntimeException(t);
            }

            if (firstStart.computeIfAbsent(devId, x -> true)) {
                Runnable closeTask = () -> {
                    DevServicesResultBuildItem.RunningDevService current = devServices.get(devId);
                    if (current != null) {
                        try {
                            current.close();
                        } catch (Throwable t) {
                            log.errorf("Failed to stop %s container", devId, t);
                        }
                    }
                    firstStart.remove(devId);
                    //noinspection resource
                    devServices.remove(devId);
                    capturedDevServicesConfigurations.remove(devId);
                };
                closeBuildItem.addCloseTask(closeTask, true);
            }

            services.produce(devService.toBuildItem());
        });
    }

    private DevServicesResultBuildItem.RunningDevService startContainer(
            String devId,
            DevResourceLifecycleManager<ContainerConfig> dev,
            ContainerConfig capturedDevServicesConfiguration,
            ModulesConfiguration root,
            Optional<Duration> timeout) {

        if (!capturedDevServicesConfiguration.enabled()) {
            // explicitly disabled
            log.debugf("Not starting Dev Services for %s as it has been disabled in the config", devId);
            return null;
        }

        if (!dev.enable()) {
            return null;
        }

        final Supplier<DevServicesResultBuildItem.RunningDevService> defaultContainerSupplier = () -> {
            GenericContainer<?> container = dev.container(capturedDevServicesConfiguration, root);
            timeout.ifPresent(container::withStartupTimeout);
            Map<String, String> config = dev.start();
            log.infof("Dev Service %s started, config: %s", devId, config);
            return new DevServicesResultBuildItem.RunningDevService(
                    Feature.OBSERVABILITY.getName(), container.getContainerId(),
                    new ContainerShutdownCloseable(container, capturedDevServicesConfiguration.serviceName()), config);
        };

        Map<String, String> config = new LinkedHashMap<>(); // old config
        ContainerLocator containerLocator = new ContainerLocator(capturedDevServicesConfiguration.label(), 0); // can be 0, as we don't use it
        return containerLocator
                .locateContainer(
                        capturedDevServicesConfiguration.serviceName(), capturedDevServicesConfiguration.shared(),
                        LaunchMode.current(), (p, ca) -> config.putAll(dev.config(p, ca.getHost(), ca.getPort())))
                .map(cid -> {
                    log.infof("Dev Service %s re-used, config: %s", devId, config);
                    return new DevServicesResultBuildItem.RunningDevService(Feature.OBSERVABILITY.getName(), cid,
                            null, config);
                })
                .orElseGet(defaultContainerSupplier);
    }

}
