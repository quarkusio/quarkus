package io.quarkus.kafka.client.deployment;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesNativeConfigResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Starts a Kafka broker as dev service if needed.
 */
public class DevServicesKafkaProcessor {

    private static final Logger log = Logger.getLogger(DevServicesKafkaProcessor.class);
    private static final int KAFKA_PORT = 9092;
    private static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";

    /**
     * Label to add to shared Dev Service for Kafka running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-kafka";

    private static final ContainerLocator kafkaContainerLocator = new ContainerLocator(DEV_SERVICE_LABEL, KAFKA_PORT);

    static volatile Closeable closeable;
    static volatile KafkaDevServiceCfg cfg;
    static volatile boolean first = true;

    private final IsDockerWorking isDockerWorking = new IsDockerWorking(true);

    @BuildStep(onlyIfNot = IsNormal.class)
    public DevServicesKafkaBrokerBuildItem startKafkaDevService(
            LaunchModeBuildItem launchMode,
            KafkaBuildTimeConfig kafkaClientBuildTimeConfig,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfiguration,
            BuildProducer<DevServicesNativeConfigResultBuildItem> devServicePropertiesProducer,
            BuildProducer<ServiceStartBuildItem> serviceStartBuildItemBuildProducer) {

        KafkaDevServiceCfg configuration = getConfiguration(kafkaClientBuildTimeConfig);

        if (closeable != null) {
            boolean shouldShutdownTheBroker = launchMode.getLaunchMode() == LaunchMode.TEST;
            if (!shouldShutdownTheBroker) {
                shouldShutdownTheBroker = !configuration.equals(cfg);
            }
            if (!shouldShutdownTheBroker) {
                return null;
            }
            shutdownBroker();
            cfg = null;
        }

        KafkaBroker kafkaBroker = startKafka(configuration, launchMode);
        DevServicesKafkaBrokerBuildItem bootstrapServers = null;
        if (kafkaBroker != null) {
            closeable = kafkaBroker.getCloseable();
            runTimeConfiguration.produce(new RunTimeConfigurationDefaultBuildItem(
                    KAFKA_BOOTSTRAP_SERVERS, kafkaBroker.getBootstrapServers()));
            bootstrapServers = new DevServicesKafkaBrokerBuildItem(kafkaBroker.getBootstrapServers());
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (closeable != null) {
                    shutdownBroker();
                }
                first = true;
                closeable = null;
                cfg = null;
            };
            QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
            ((QuarkusClassLoader) cl.parent()).addCloseTask(closeTask);
            Thread closeHookThread = new Thread(closeTask, "Kafka container shutdown thread");
            Runtime.getRuntime().addShutdownHook(closeHookThread);
            ((QuarkusClassLoader) cl.parent()).addCloseTask(() -> Runtime.getRuntime().removeShutdownHook(closeHookThread));
        }
        cfg = configuration;

        if (bootstrapServers != null) {
            if (kafkaBroker.isOwner()) {
                log.infof(
                        "Dev Services for Kafka started. Other Quarkus applications in dev mode will find the "
                                + "broker automatically. For Quarkus applications in production mode, you can connect to"
                                + " this by starting your application with -Dkafka.bootstrap.servers=%s",
                        bootstrapServers.getBootstrapServers());
            }

            devServicePropertiesProducer.produce(new DevServicesNativeConfigResultBuildItem("kafka.bootstrap.servers",
                    bootstrapServers.getBootstrapServers()));
        }

        return bootstrapServers;
    }

    private void shutdownBroker() {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable e) {
                log.error("Failed to stop the Kafka broker", e);
            } finally {
                closeable = null;
            }
        }
    }

    private KafkaBroker startKafka(KafkaDevServiceCfg config,
            LaunchModeBuildItem launchMode) {
        if (!config.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting dev services for Kafka, as it has been disabled in the config.");
            return null;
        }

        // Check if kafka.bootstrap.servers is set
        if (ConfigUtils.isPropertyPresent(KAFKA_BOOTSTRAP_SERVERS)) {
            log.debug("Not starting dev services for Kafka, the kafka.bootstrap.servers is configured.");
            return null;
        }

        // Verify that we have kafka channels without bootstrap.servers
        if (!hasKafkaChannelWithoutBootstrapServers()) {
            log.debug("Not starting dev services for Kafka, all the channels are configured.");
            return null;
        }

        if (!isDockerWorking.getAsBoolean()) {
            log.warn(
                    "Docker isn't working, please configure the Kafka bootstrap servers property (kafka.bootstrap.servers).");
            return null;
        }

        final Optional<ContainerAddress> maybeContainerAddress = kafkaContainerLocator.locateContainer(config.serviceName,
                config.shared,
                launchMode.getLaunchMode());

        // Starting the broker
        final Supplier<KafkaBroker> defaultKafkaBrokerSupplier = () -> {
            RedPandaKafkaContainer container = new RedPandaKafkaContainer(
                    DockerImageName.parse(config.imageName),
                    config.fixedExposedPort,
                    launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName : null);
            container.start();

            return new KafkaBroker(
                    container.getBootstrapServers(),
                    container::close);
        };

        return maybeContainerAddress.map(containerAddress -> new KafkaBroker(containerAddress.getUrl(), null))
                .orElseGet(defaultKafkaBrokerSupplier);
    }

    private boolean hasKafkaChannelWithoutBootstrapServers() {
        Config config = ConfigProvider.getConfig();
        for (String name : config.getPropertyNames()) {
            boolean isIncoming = name.startsWith("mp.messaging.incoming.");
            boolean isOutgoing = name.startsWith("mp.messaging.outgoing.");
            boolean isConnector = name.endsWith(".connector");
            boolean isKafka = isConnector
                    && "smallrye-kafka".equals(config.getOptionalValue(name, String.class).orElse("ignored"));
            boolean isConfigured = false;
            if ((isIncoming || isOutgoing) && isKafka) {
                isConfigured = ConfigUtils.isPropertyPresent(name.replace(".connector", ".bootstrap.servers"));
            }
            if (!isConfigured) {
                return true;
            }
        }
        return false;
    }

    private KafkaDevServiceCfg getConfiguration(KafkaBuildTimeConfig cfg) {
        KafkaDevServicesBuildTimeConfig devServicesConfig = cfg.devservices;
        return new KafkaDevServiceCfg(devServicesConfig);
    }

    private static class KafkaBroker {
        private final String url;
        private final Closeable closeable;

        public KafkaBroker(String url, Closeable closeable) {
            this.url = url;
            this.closeable = closeable;
        }

        public boolean isOwner() {
            return closeable != null;
        }

        public String getBootstrapServers() {
            return url;
        }

        public Closeable getCloseable() {
            return closeable;
        }
    }

    private static final class KafkaDevServiceCfg {
        private final boolean devServicesEnabled;
        private final String imageName;
        private final Integer fixedExposedPort;
        private final boolean shared;
        private final String serviceName;

        public KafkaDevServiceCfg(KafkaDevServicesBuildTimeConfig config) {
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
            KafkaDevServiceCfg that = (KafkaDevServiceCfg) o;
            return devServicesEnabled == that.devServicesEnabled && Objects.equals(imageName, that.imageName)
                    && Objects.equals(fixedExposedPort, that.fixedExposedPort);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, imageName, fixedExposedPort);
        }
    }

    /**
     * Container configuring and starting the Redpanda broker.
     * See https://vectorized.io/docs/quick-start-docker/
     */
    private static final class RedPandaKafkaContainer extends GenericContainer<RedPandaKafkaContainer> {

        private final int port;

        private static final String STARTER_SCRIPT = "/redpanda.sh";

        private RedPandaKafkaContainer(DockerImageName dockerImageName, int fixedExposedPort, String serviceName) {
            super(dockerImageName);
            this.port = fixedExposedPort;
            withNetwork(Network.SHARED);
            withExposedPorts(KAFKA_PORT);
            if (serviceName != null) { // Only adds the label in dev mode.
                withLabel(DEV_SERVICE_LABEL, serviceName);
            }
            // For redpanda, we need to start the broker - see https://vectorized.io/docs/quick-start-docker/
            if (dockerImageName.getRepository().equals("vectorized/redpanda")) {
                withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint("sh");
                });
                withCommand("-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);
                waitingFor(Wait.forLogMessage(".*Started Kafka API server.*", 1));
            } else {
                throw new IllegalArgumentException("Only vectorized/redpanda images are supported");
            }
        }

        @Override
        protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
            super.containerIsStarting(containerInfo, reused);

            // Start and configure the advertised address
            String command = "#!/bin/bash\n";
            command += "/usr/bin/rpk redpanda start --check=false --node-id 0 ";
            command += "--kafka-addr PLAINTEXT://0.0.0.0:29092,OUTSIDE://0.0.0.0:9092 ";
            command += "--advertise-kafka-addr PLAINTEXT://kafka:29092,OUTSIDE://" + getHost() + ":" + getMappedPort(9092);

            //noinspection OctalInteger
            copyFileToContainer(
                    Transferable.of(command.getBytes(StandardCharsets.UTF_8), 0777),
                    STARTER_SCRIPT);
        }

        @Override
        protected void configure() {
            super.configure();
            if (port > 0) {
                addFixedExposedPort(port, KAFKA_PORT);
            }
        }

        public String getBootstrapServers() {
            return String.format("PLAINTEXT://%s:%s", getContainerIpAddress(), getMappedPort(KAFKA_PORT));
        }
    }
}
