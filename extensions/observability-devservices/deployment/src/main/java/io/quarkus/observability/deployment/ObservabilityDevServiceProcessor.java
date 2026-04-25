package io.quarkus.observability.deployment;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.dev.devservices.ContainerInfo;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.dev.devservices.RunningContainer;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.observability.common.config.ContainerConfig;
import io.quarkus.observability.common.config.ContainerConfigUtil;
import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.observability.devresource.Container;
import io.quarkus.observability.devresource.DevResourceLifecycleManager;
import io.quarkus.observability.devresource.DevResources;
import io.quarkus.observability.devresource.ExtensionsCatalog;
import io.quarkus.observability.runtime.config.ObservabilityConfiguration;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.metrics.MetricsFactory;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class,
        ObservabilityDevServiceProcessor.IsEnabled.class })
class ObservabilityDevServiceProcessor {
    private static final Logger log = Logger.getLogger(ObservabilityDevServiceProcessor.class);

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
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            DevServicesConfig devServicesConfig,
            BuildProducer<DevServicesResultBuildItem> services,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> properties,
            Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration) {

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

        ExtensionsCatalog catalog = new ExtensionsCatalog(
                QuarkusClassLoader::isResourcePresentAtRuntime,
                QuarkusClassLoader::isClassPresentAtRuntime,
                capabilities.isPresent(Capability.OPENTELEMETRY_TRACER) ||
                        capabilities.isPresent(Capability.OPENTELEMETRY_METRICS) ||
                        capabilities.isPresent(Capability.OPENTELEMETRY_LOGS),
                hasMicrometerOtlp(metricsConfiguration));

        for (DevResourceLifecycleManager<ContainerConfig> dev : resources) {
            String devId = devId(dev);

            ContainerConfig currentDevServicesConfiguration = dev.config(configuration, catalog);

            if (!currentDevServicesConfiguration.enabled()) {
                log.debugf("Not starting Dev Services for %s as it has been disabled in the config", devId);
                continue;
            }

            if (!dev.enable()) {
                continue;
            }

            Map<String, Object> propertiesToOverride = ContainerConfigUtil
                    .propertiesToOverride(currentDevServicesConfiguration);
            propertiesToOverride
                    .forEach((k, v) -> properties.produce(new RunTimeConfigurationDefaultBuildItem(k, v.toString())));
            log.infof("Dev Service %s properties override: %s", devId, propertiesToOverride);

            DevServicesResultBuildItem discovered = discoverRunningService(dev, devId, composeProjectBuildItem,
                    currentDevServicesConfiguration, launchMode.getLaunchMode());
            if (discovered != null) {
                services.produce(discovered);
            } else {
                services.produce(
                        DevServicesResultBuildItem.owned()
                                .feature(Feature.OBSERVABILITY)
                                .serviceName(devId)
                                .serviceConfig(currentDevServicesConfiguration)
                                .startable(() -> new ObservabilityStartable(dev, currentDevServicesConfiguration,
                                        configuration, devServicesConfig.timeout()))
                                .configResolver(ObservabilityStartable::getDevServiceConfig)
                                .postStartHook(s -> log.infof("Dev Service %s started, config: %s",
                                        devId, s.getDevServiceConfig()))
                                .build());
            }
        }
    }

    private DevServicesResultBuildItem discoverRunningService(
            DevResourceLifecycleManager<ContainerConfig> dev, String devId,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            ContainerConfig capturedDevServicesConfiguration, LaunchMode launchMode) {

        ContainerLocator containerLocator = new ContainerLocator(capturedDevServicesConfiguration.label(), 0);
        Map<String, String> config = new LinkedHashMap<>();
        Optional<String> containerId = containerLocator
                .locateContainer(capturedDevServicesConfiguration.serviceName(),
                        capturedDevServicesConfiguration.shared(),
                        launchMode,
                        (p, ca) -> config.putAll(dev.config(p, ca.getHost(), ca.getPort())));

        if (containerId.isPresent()) {
            log.infof("Dev Service %s re-used, config: %s", devId, config);
            return DevServicesResultBuildItem.discovered()
                    .feature(Feature.OBSERVABILITY)
                    .containerId(containerId.get())
                    .config(config)
                    .build();
        }

        List<RunningContainer> composeContainers = ComposeLocator.locateContainer(composeProjectBuildItem,
                List.of(capturedDevServicesConfiguration.imageName(), capturedDevServicesConfiguration.serviceName()),
                launchMode);
        if (!composeContainers.isEmpty()) {
            RunningContainer r = composeContainers.get(0);
            Map<String, String> cfg = new LinkedHashMap<>();
            for (ContainerInfo.ContainerPort port : r.containerInfo().exposedPorts()) {
                cfg.putAll(dev.config(port.privatePort(),
                        DockerClientFactory.instance().dockerHostIpAddress(),
                        port.publicPort()));
            }
            log.infof("Compose Dev Service %s started, config: %s", devId, cfg);
            return DevServicesResultBuildItem.discovered()
                    .feature(Feature.OBSERVABILITY)
                    .containerId(r.containerInfo().id())
                    .config(cfg)
                    .build();
        }

        return null;
    }

    private static boolean hasMicrometerOtlp(Optional<MetricsCapabilityBuildItem> metricsConfiguration) {
        if (metricsConfiguration.isPresent() &&
                metricsConfiguration.get().metricsSupported(MetricsFactory.MICROMETER)) {
            return QuarkusClassLoader.isClassPresentAtRuntime(OTLP_REGISTRY.toString());
        }
        return false;
    }

    static final class ObservabilityStartable implements Startable {
        private final DevResourceLifecycleManager<ContainerConfig> dev;
        private final ContainerConfig config;
        private final ModulesConfiguration root;
        private final Optional<Duration> timeout;

        private Container<?> container;
        private Map<String, String> devServiceConfig;

        ObservabilityStartable(DevResourceLifecycleManager<ContainerConfig> dev,
                ContainerConfig config, ModulesConfiguration root, Optional<Duration> timeout) {
            this.dev = dev;
            this.config = config;
            this.root = root;
            this.timeout = timeout;
        }

        @Override
        public void start() {
            container = dev.container(config, root);
            timeout.ifPresent(container::withStartupTimeout);
            devServiceConfig = dev.start();
        }

        @Override
        public String getConnectionInfo() {
            return container != null ? container.getContainerId() : "not started";
        }

        @Override
        public String getContainerId() {
            return container != null ? container.getContainerId() : null;
        }

        @Override
        public void close() throws IOException {
            if (container != null) {
                container.closeableCallback(config.serviceName()).close();
            }
        }

        public Map<String, String> getDevServiceConfig() {
            return devServiceConfig != null ? devServiceConfig : Map.of();
        }
    }
}
