package io.quarkus.apicurio.registry.avro;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts Apicurio Registry as dev service if needed.
 * <p>
 * In the future, when we have multiple Apicurio Registry extensions (Avro, Protobuf, ...),
 * this dev service support should probably be moved into an extra extension (quarkus-apicurio-registry-internal).
 */
public class DevServicesApicurioRegistryProcessor {

    private static final Logger log = Logger.getLogger(DevServicesApicurioRegistryProcessor.class);

    private static final int APICURIO_REGISTRY_PORT = 8080; // inside the container
    private static final String REGISTRY_URL_CONFIG = "mp.messaging.connector.smallrye-kafka.apicurio.registry.url";

    /**
     * Label to add to shared Dev Service for Apicurio Registry running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-apicurio-registry";

    private static final ContainerLocator apicurioRegistryContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL,
            APICURIO_REGISTRY_PORT);

    static volatile AutoCloseable closeable;
    static volatile ApicurioRegistryDevServiceCfg cfg;
    static volatile boolean first = true;

    private final IsDockerWorking isDockerWorking = new IsDockerWorking(true);

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
    public void startApicurioRegistryDevService(LaunchModeBuildItem launchMode,
            ApicurioRegistryDevServicesBuildTimeConfig apicurioRegistryDevServices,
            Optional<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            BuildProducer<DevServicesConfigResultBuildItem> devServicesConfiguration) {

        ApicurioRegistryDevServiceCfg configuration = getConfiguration(apicurioRegistryDevServices);

        if (closeable != null) {
            boolean restartRequired = !configuration.equals(cfg);
            if (!restartRequired) {
                return;
            }
            shutdownApicurioRegistry();
            cfg = null;
        }

        ApicurioRegistry apicurioRegistry = startApicurioRegistry(configuration, launchMode,
                devServicesSharedNetworkBuildItem.isPresent());
        if (apicurioRegistry == null) {
            return;
        }

        cfg = configuration;
        closeable = apicurioRegistry.getCloseable();

        devServicesConfiguration.produce(new DevServicesConfigResultBuildItem(
                REGISTRY_URL_CONFIG, apicurioRegistry.getUrl() + "/apis/registry/v2"));

        if (apicurioRegistry.isOwner()) {
            log.infof("Dev Services for Apicurio Registry started. The registry is available at %s", apicurioRegistry.getUrl());
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = new Runnable() {
                @Override
                public void run() {
                    if (closeable != null) {
                        shutdownApicurioRegistry();
                    }
                    first = true;
                    closeable = null;
                    cfg = null;
                }
            };
            QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
            ((QuarkusClassLoader) cl.parent()).addCloseTask(closeTask);
            Thread closeHookThread = new Thread(closeTask, "Apicurio Registry container shutdown thread");
            Runtime.getRuntime().addShutdownHook(closeHookThread);
            ((QuarkusClassLoader) cl.parent()).addCloseTask(new Runnable() {
                @Override
                public void run() {
                    Runtime.getRuntime().removeShutdownHook(closeHookThread);
                }
            });
        }
    }

    private void shutdownApicurioRegistry() {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable e) {
                log.error("Failed to stop Apicurio Registry", e);
            } finally {
                closeable = null;
            }
        }
    }

    private ApicurioRegistry startApicurioRegistry(ApicurioRegistryDevServiceCfg config, LaunchModeBuildItem launchMode,
            boolean useSharedNetwork) {
        if (!config.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting dev services for Apicurio Registry, as it has been disabled in the config.");
            return null;
        }

        if (ConfigUtils.isPropertyPresent(REGISTRY_URL_CONFIG)) {
            log.debug("Not starting dev services for Apicurio Registry, " + REGISTRY_URL_CONFIG + " is configured.");
            return null;
        }

        if (!hasKafkaChannelWithoutApicurioRegistry()) {
            log.debug("Not starting dev services for Apicurio Registry, all the channels have a registry URL configured.");
            return null;
        }

        if (!isDockerWorking.getAsBoolean()) {
            log.warn("Docker isn't working, please run Apicurio Registry yourself.");
            return null;
        }

        // Starting the broker
        return apicurioRegistryContainerLocator.locateContainer(config.serviceName, config.shared, launchMode.getLaunchMode())
                .map(containerAddress -> new ApicurioRegistry(containerAddress.getUrl(), null))
                .orElseGet(() -> {
                    ApicurioRegistryContainer container = new ApicurioRegistryContainer(
                            DockerImageName.parse(config.imageName), config.fixedExposedPort,
                            launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName : null,
                            useSharedNetwork);
                    container.start();

                    return new ApicurioRegistry(container.getUrl(), container);
                });
    }

    private boolean hasKafkaChannelWithoutApicurioRegistry() {
        Config config = ConfigProvider.getConfig();
        for (String name : config.getPropertyNames()) {
            boolean isIncoming = name.startsWith("mp.messaging.incoming.");
            boolean isOutgoing = name.startsWith("mp.messaging.outgoing.");
            boolean isConnector = name.endsWith(".connector");
            boolean isKafka = isConnector
                    && "smallrye-kafka".equals(config.getOptionalValue(name, String.class).orElse("ignored"));
            boolean isConfigured = false;
            if ((isIncoming || isOutgoing) && isKafka) {
                isConfigured = ConfigUtils.isPropertyPresent(name.replace(".connector", ".apicurio.registry.url"));
            }
            if (!isConfigured) {
                return true;
            }
        }
        return false;
    }

    private ApicurioRegistryDevServiceCfg getConfiguration(ApicurioRegistryDevServicesBuildTimeConfig cfg) {
        return new ApicurioRegistryDevServiceCfg(cfg);
    }

    private static class ApicurioRegistry {
        private final String url;
        private final AutoCloseable closeable;

        public ApicurioRegistry(String url, AutoCloseable closeable) {
            this.url = url;
            this.closeable = closeable;
        }

        public String getUrl() {
            return url;
        }

        public AutoCloseable getCloseable() {
            return closeable;
        }

        public boolean isOwner() {
            return closeable != null;
        }
    }

    private static final class ApicurioRegistryDevServiceCfg {
        private final boolean devServicesEnabled;
        private final String imageName;
        private final Integer fixedExposedPort;
        private final boolean shared;
        private final String serviceName;

        public ApicurioRegistryDevServiceCfg(ApicurioRegistryDevServicesBuildTimeConfig config) {
            this.devServicesEnabled = config.enabled.orElse(true);
            this.imageName = config.imageName;
            this.fixedExposedPort = config.port.orElse(0);
            this.shared = config.shared;
            this.serviceName = config.serviceName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ApicurioRegistryDevServiceCfg that = (ApicurioRegistryDevServiceCfg) o;
            return devServicesEnabled == that.devServicesEnabled
                    && Objects.equals(imageName, that.imageName)
                    && Objects.equals(fixedExposedPort, that.fixedExposedPort)
                    && shared == that.shared
                    && Objects.equals(serviceName, that.serviceName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, imageName, fixedExposedPort, shared, serviceName);
        }
    }

    private static final class ApicurioRegistryContainer extends GenericContainer<ApicurioRegistryContainer> {
        private final int port;
        private final boolean useSharedNetwork;

        private String hostName = null;

        private ApicurioRegistryContainer(DockerImageName dockerImageName, int fixedExposedPort, String serviceName,
                boolean useSharedNetwork) {
            super(dockerImageName);
            this.port = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            withNetwork(Network.SHARED);
            if (useSharedNetwork) {
                hostName = "kafka-" + Base58.randomString(5);
                setNetworkAliases(Collections.singletonList(hostName));
            } else {
                withExposedPorts(APICURIO_REGISTRY_PORT);
            }
            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
            withEnv("QUARKUS_PROFILE", "prod");
            if (!dockerImageName.getRepository().equals("apicurio/apicurio-registry-mem")) {
                throw new IllegalArgumentException("Only apicurio/apicurio-registry-mem images are supported");
            }
        }

        @Override
        protected void configure() {
            super.configure();
            if (port > 0 && !useSharedNetwork) {
                addFixedExposedPort(port, APICURIO_REGISTRY_PORT);
            }
        }

        public String getUrl() {
            return String.format("http://%s:%s", getHostToUse(), getPortToUse());
        }

        private String getHostToUse() {
            return useSharedNetwork ? hostName : getHost();
        }

        private int getPortToUse() {
            return useSharedNetwork ? APICURIO_REGISTRY_PORT : getMappedPort(APICURIO_REGISTRY_PORT);
        }
    }
}
