package io.quarkus.apicurio.registry.avro;

import java.util.Objects;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesNativeConfigResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts Apicurio Registry as dev service if needed.
 * <p>
 * In the future, when we have multiple Apicurio Registry extensions (Avro, Protobuf, ...),
 * this dev service support should probably be moved into an extra extension (quarkus-apicurio-registry-internal).
 */
public class DevServicesApicurioRegistryProcessor {

    private static final Logger log = Logger.getLogger(DevServicesApicurioRegistryProcessor.class);

    private static final String REGISTRY_URL_CONFIG = "mp.messaging.connector.smallrye-kafka.apicurio.registry.url";

    static volatile AutoCloseable closeable;
    static volatile ApicurioRegistryDevServiceCfg cfg;
    static volatile boolean first = true;

    private final IsDockerWorking isDockerWorking = new IsDockerWorking(true);

    @BuildStep(onlyIfNot = IsNormal.class)
    public void startApicurioRegistryDevService(
            LaunchModeBuildItem launchMode,
            ApicurioRegistryDevServicesBuildTimeConfig apicurioRegistryDevServices,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfiguration,
            BuildProducer<DevServicesNativeConfigResultBuildItem> devServicesConfiguration) {

        ApicurioRegistryDevServiceCfg configuration = getConfiguration(apicurioRegistryDevServices);

        if (closeable != null) {
            boolean restartRequired = !configuration.equals(cfg);
            if (!restartRequired) {
                return;
            }
            shutdownApicurioRegistry();
            cfg = null;
        }

        ApicurioRegistry apicurioRegistry = startApicurioRegistry(configuration);
        if (apicurioRegistry == null) {
            return;
        }

        cfg = configuration;
        closeable = apicurioRegistry.getCloseable();

        runTimeConfiguration.produce(new RunTimeConfigurationDefaultBuildItem(
                REGISTRY_URL_CONFIG, apicurioRegistry.getUrl() + "/apis/registry/v2"));
        devServicesConfiguration.produce(new DevServicesNativeConfigResultBuildItem(
                REGISTRY_URL_CONFIG, apicurioRegistry.getUrl() + "/apis/registry/v2"));

        log.infof("Dev Services for Apicurio Registry started. The registry is available at %s", apicurioRegistry.getUrl());

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

    private ApicurioRegistry startApicurioRegistry(ApicurioRegistryDevServiceCfg config) {
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
        ApicurioRegistryContainer container = new ApicurioRegistryContainer(
                DockerImageName.parse(config.imageName), config.fixedExposedPort);
        container.start();

        return new ApicurioRegistry(container.getUrl(), container);
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
        return new ApicurioRegistryDevServiceCfg(cfg.enabled.orElse(true), cfg.imageName, cfg.port.orElse(0));
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
    }

    private static final class ApicurioRegistryDevServiceCfg {
        private final boolean devServicesEnabled;
        private final String imageName;
        private final Integer fixedExposedPort;

        public ApicurioRegistryDevServiceCfg(boolean devServicesEnabled, String imageName, Integer fixedExposedPort) {
            this.devServicesEnabled = devServicesEnabled;
            this.imageName = imageName;
            this.fixedExposedPort = fixedExposedPort;
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
                    && Objects.equals(fixedExposedPort, that.fixedExposedPort);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, imageName, fixedExposedPort);
        }
    }

    private static final class ApicurioRegistryContainer extends GenericContainer<ApicurioRegistryContainer> {
        private static final int APICURIO_REGISTRY_PORT = 8080; // inside the container

        private final int port;

        private ApicurioRegistryContainer(DockerImageName dockerImageName, int fixedExposedPort) {
            super(dockerImageName);
            this.port = fixedExposedPort;
            withNetwork(Network.SHARED);
            withExposedPorts(APICURIO_REGISTRY_PORT);
            withEnv("QUARKUS_PROFILE", "prod");
            if (!dockerImageName.getRepository().equals("apicurio/apicurio-registry-mem")) {
                throw new IllegalArgumentException("Only apicurio/apicurio-registry-mem images are supported");
            }
        }

        @Override
        protected void configure() {
            super.configure();
            if (port > 0) {
                addFixedExposedPort(port, APICURIO_REGISTRY_PORT);
            }
        }

        public String getUrl() {
            return String.format("http://%s:%s", getContainerIpAddress(), getMappedPort(APICURIO_REGISTRY_PORT));
        }
    }
}
