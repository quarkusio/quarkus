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

import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.ContainerInfo;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.observability.common.config.ContainerConfig;
import io.quarkus.observability.common.config.ContainerConfigUtil;
import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.observability.deployment.devui.ObservabilityDevServicesConfigBuildItem;
import io.quarkus.observability.devresource.Container;
import io.quarkus.observability.devresource.DevResourceLifecycleManager;
import io.quarkus.observability.devresource.DevResources;
import io.quarkus.observability.devresource.ExtensionsCatalog;
import io.quarkus.observability.runtime.config.ObservabilityConfiguration;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.metrics.MetricsFactory;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { DevServicesConfig.Enabled.class,
        ObservabilityDevServiceProcessor.IsEnabled.class })
class ObservabilityDevServiceProcessor {
    private static final Logger log = Logger.getLogger(ObservabilityDevServiceProcessor.class);

    private static final Map<String, DevServicesResultBuildItem.RunningDevService> devServices = new ConcurrentHashMap<>();
    private static final Map<String, ContainerConfig> capturedDevServicesConfigurations = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> firstStart = new ConcurrentHashMap<>();
    private static final DotName OTLP_REGISTRY = DotName.createSimple("io.micrometer.registry.otlp.OtlpMeterRegistry");

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
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig,
            BuildProducer<DevServicesResultBuildItem> services,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> properties,
            Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            BuildProducer<ObservabilityDevServicesConfigBuildItem> configBuildProducer) {

        if (!configuration.enabled()) {
            log.infof("Observability dev services are disabled in config");
            return;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
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

            // only do get, not remove, so it can be re-used
            DevServicesResultBuildItem.RunningDevService devService = devServices.get(devId);
            ContainerConfig currentDevServicesConfiguration = dev.config(
                    configuration,
                    new ExtensionsCatalog(
                            QuarkusClassLoader::isResourcePresentAtRuntime,
                            QuarkusClassLoader::isClassPresentAtRuntime,
                            capabilities.isPresent(Capability.OPENTELEMETRY_TRACER) ||
                                    capabilities.isPresent(Capability.OPENTELEMETRY_METRICS) ||
                                    capabilities.isPresent(Capability.OPENTELEMETRY_LOGS),
                            hasMicrometerOtlp(metricsConfiguration)));

            if (devService != null) {
                ContainerConfig capturedDevServicesConfiguration = capturedDevServicesConfigurations.get(devId);
                boolean equalConfig = ContainerConfigUtil.isEqual(capturedDevServicesConfiguration,
                        currentDevServicesConfiguration);
                if (equalConfig) {
                    log.debugf("Equal config, re-using existing %s container", devId);
                    services.produce(devService.toBuildItem());
                    return;
                }
                try {
                    devService.close();
                } catch (Throwable e) {
                    log.errorf("Failed to stop %s container", devId, e);
                }
            }

            devServices.remove(devId); // clean-up
            capturedDevServicesConfigurations.put(devId, currentDevServicesConfiguration);

            // override some OTel, etc defaults - rates, intervals, delays, ...
            Map<String, Object> propertiesToOverride = ContainerConfigUtil
                    .propertiesToOverride(currentDevServicesConfiguration);
            propertiesToOverride
                    .forEach((k, v) -> properties.produce(new RunTimeConfigurationDefaultBuildItem(k, v.toString())));
            log.infof("Dev Service %s properties override: %s", devId, propertiesToOverride);

            StartupLogCompressor compressor = new StartupLogCompressor(
                    (launchMode.isTest() ? "(test) " : "") + devId + " Dev Services Starting:",
                    consoleInstalledBuildItem,
                    loggingSetupBuildItem,
                    s -> false,
                    s -> s.contains(getClass().getSimpleName()) ||
                            s.contains("Resource") ||
                            s.contains("Container")); // log if it comes from this class or Resource / Container
            try {
                DevServicesResultBuildItem.RunningDevService newDevService = startContainer(
                        devId,
                        composeProjectBuildItem,
                        dev,
                        currentDevServicesConfiguration,
                        configuration,
                        devServicesConfig.timeout());
                if (newDevService == null) {
                    compressor.closeAndDumpCaptured();
                    return;
                } else {
                    compressor.close();
                }

                devService = newDevService;
                devServices.put(devId, newDevService);
                configBuildProducer.produce(new ObservabilityDevServicesConfigBuildItem(newDevService.getConfig()));
            } catch (Throwable t) {
                compressor.closeAndDumpCaptured();
                throw new RuntimeException(t);
            }

            if (firstStart.computeIfAbsent(devId, x -> true)) {
                Runnable closeTask = new Runnable() {
                    @Override
                    public void run() {
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
                    }
                };
                closeBuildItem.addCloseTask(closeTask, true);
            }

            services.produce(devService.toBuildItem());
        });
    }

    private static boolean hasMicrometerOtlp(Optional<MetricsCapabilityBuildItem> metricsConfiguration) {
        if (metricsConfiguration.isPresent() &&
                metricsConfiguration.get().metricsSupported(MetricsFactory.MICROMETER)) {
            return QuarkusClassLoader.isClassPresentAtRuntime(OTLP_REGISTRY.toString());
        }
        return false;
    }

    private DevServicesResultBuildItem.RunningDevService startContainer(
            String devId,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
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

        final Supplier<DevServicesResultBuildItem.RunningDevService> defaultContainerSupplier = new Supplier<DevServicesResultBuildItem.RunningDevService>() {
            @Override
            public DevServicesResultBuildItem.RunningDevService get() {
                Container<?> container = dev.container(capturedDevServicesConfiguration, root);
                timeout.ifPresent(container::withStartupTimeout);
                Map<String, String> config = dev.start();
                log.infof("Dev Service %s started, config: %s", devId, config);
                return new DevServicesResultBuildItem.RunningDevService(
                        Feature.OBSERVABILITY.getName(), container.getContainerId(),
                        container.closeableCallback(capturedDevServicesConfiguration.serviceName()), config);
            }
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
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(capturedDevServicesConfiguration.imageName(), capturedDevServicesConfiguration.serviceName()),
                        LaunchMode.current())
                        .stream().findFirst()
                        .map(r -> {
                            Map<String, String> cfg = new LinkedHashMap<>();
                            for (ContainerInfo.ContainerPort port : r.containerInfo().exposedPorts()) {
                                cfg.putAll(dev.config(port.privatePort(),
                                        DockerClientFactory.instance().dockerHostIpAddress(),
                                        port.publicPort()));
                            }
                            log.infof("Compose Dev Service %s started, config: %s", devId, cfg);
                            return new DevServicesResultBuildItem.RunningDevService(Feature.OBSERVABILITY.getName(),
                                    r.containerInfo().id(), null, cfg);
                        }))
                .orElseGet(defaultContainerSupplier);
    }
}
