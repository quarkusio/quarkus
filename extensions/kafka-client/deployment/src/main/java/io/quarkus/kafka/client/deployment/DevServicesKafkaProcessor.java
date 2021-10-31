package io.quarkus.kafka.client.deployment;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
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

    @BuildStep(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
    public DevServicesKafkaBrokerBuildItem startKafkaDevService(
            LaunchModeBuildItem launchMode,
            KafkaBuildTimeConfig kafkaClientBuildTimeConfig,
            Optional<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            BuildProducer<DevServicesConfigResultBuildItem> devServicePropertiesProducer,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem, GlobalDevServicesConfig devServicesConfig) {

        KafkaDevServiceCfg configuration = getConfiguration(kafkaClientBuildTimeConfig);

        if (closeable != null) {
            boolean shouldShutdownTheBroker = !configuration.equals(cfg);
            if (!shouldShutdownTheBroker) {
                return null;
            }
            shutdownBroker();
            cfg = null;
        }
        KafkaBroker kafkaBroker;
        DevServicesKafkaBrokerBuildItem bootstrapServers;
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Kafka Dev Services Starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);
        try {

            kafkaBroker = startKafka(configuration, launchMode, devServicesSharedNetworkBuildItem.isPresent(),
                    devServicesConfig.timeout);
            bootstrapServers = null;
            if (kafkaBroker != null) {
                closeable = kafkaBroker.getCloseable();
                devServicePropertiesProducer.produce(new DevServicesConfigResultBuildItem(
                        KAFKA_BOOTSTRAP_SERVERS, kafkaBroker.getBootstrapServers()));
                bootstrapServers = new DevServicesKafkaBrokerBuildItem(kafkaBroker.getBootstrapServers());
            }
            compressor.close();
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
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
            closeBuildItem.addCloseTask(closeTask, true);
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
            createTopicPartitions(bootstrapServers.getBootstrapServers(), configuration);
        }

        return bootstrapServers;
    }

    public void createTopicPartitions(String bootstrapServers, KafkaDevServiceCfg configuration) {
        Map<String, Integer> topicPartitions = configuration.topicPartitions;
        if (topicPartitions.isEmpty()) {
            return;
        }
        Map<String, Object> props = Map.ofEntries(
                Map.entry(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
                Map.entry(AdminClientConfig.CLIENT_ID_CONFIG, "kafka-devservices"));
        try (AdminClient adminClient = KafkaAdminClient.create(props)) {
            long adminClientTimeout = configuration.topicPartitionsTimeout.toMillis();
            // get current partitions for topics asked to be created
            Set<String> currentTopics = adminClient.listTopics().names()
                    .get(adminClientTimeout, TimeUnit.MILLISECONDS);
            Map<String, TopicDescription> partitions = adminClient.describeTopics(currentTopics).all()
                    .get(adminClientTimeout, TimeUnit.MILLISECONDS);
            // find new topics to create
            List<NewTopic> newTopics = topicPartitions.entrySet().stream()
                    .filter(e -> {
                        TopicDescription topicDescription = partitions.get(e.getKey());
                        if (topicDescription == null) {
                            return true;
                        } else {
                            log.warnf("Topic '%s' already exists with %s partition(s)", e.getKey(),
                                    topicDescription.partitions().size());
                            return false;
                        }
                    })
                    .map(e -> new NewTopic(e.getKey(), e.getValue(), (short) 1))
                    .collect(Collectors.toList());
            // create new topics
            CreateTopicsResult topics = adminClient.createTopics(newTopics);
            topics.all().get(adminClientTimeout, TimeUnit.MILLISECONDS);
            // print out topics after create
            HashMap<String, Integer> newTopicPartitions = new HashMap<>();
            partitions.forEach((key, value) -> newTopicPartitions.put(key, value.partitions().size()));
            newTopics.forEach(t -> newTopicPartitions.put(t.name(), t.numPartitions()));
            log.infof("Dev Services for Kafka broker contains following topics with partitions: %s", newTopicPartitions);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.errorf(e, "Failed to create topics: %s", topicPartitions);
        }
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
            LaunchModeBuildItem launchMode, boolean useSharedNetwork, Optional<Duration> timeout) {
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
                    launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName : null,
                    useSharedNetwork);
            timeout.ifPresent(container::withStartupTimeout);
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
        private final Map<String, Integer> topicPartitions;
        private final Duration topicPartitionsTimeout;

        public KafkaDevServiceCfg(KafkaDevServicesBuildTimeConfig config) {
            this.devServicesEnabled = config.enabled.orElse(true);
            this.imageName = config.imageName;
            this.fixedExposedPort = config.port.orElse(0);
            this.shared = config.shared;
            this.serviceName = config.serviceName;
            this.topicPartitions = config.topicPartitions;
            this.topicPartitionsTimeout = config.topicPartitionsTimeout;
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
        private final boolean useSharedNetwork;

        private String hostName = null;

        private static final String STARTER_SCRIPT = "/var/lib/redpanda/redpanda.sh";

        private RedPandaKafkaContainer(DockerImageName dockerImageName, int fixedExposedPort, String serviceName,
                boolean useSharedNetwork) {
            super(dockerImageName);
            this.port = fixedExposedPort;
            this.useSharedNetwork = useSharedNetwork;
            withNetwork(Network.SHARED);
            if (useSharedNetwork) {
                hostName = "kafka-" + Base58.randomString(5);
                setNetworkAliases(Collections.singletonList(hostName));
            } else {
                withExposedPorts(KAFKA_PORT);
            }
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
            command += "/usr/bin/rpk redpanda start --check=false --node-id 0 --smp 1 ";
            command += "--memory 1G --overprovisioned --reserve-memory 0M ";
            command += "--kafka-addr PLAINTEXT://0.0.0.0:29092,OUTSIDE://0.0.0.0:9092 ";
            command += String.format("--advertise-kafka-addr PLAINTEXT://%s:29092,OUTSIDE://%s:%d", getHostToUse(),
                    getHostToUse(), getPortToUse());

            //noinspection OctalInteger
            copyFileToContainer(
                    Transferable.of(command.getBytes(StandardCharsets.UTF_8), 0777),
                    STARTER_SCRIPT);
        }

        @Override
        protected void configure() {
            super.configure();
            if ((port > 0) && !useSharedNetwork) {
                addFixedExposedPort(port, KAFKA_PORT);
            }
        }

        public String getBootstrapServers() {
            return String.format("PLAINTEXT://%s:%d", getHostToUse(), getPortToUse());
        }

        private String getHostToUse() {
            return useSharedNetwork ? hostName : getHost();
        }

        private int getPortToUse() {
            return useSharedNetwork ? KAFKA_PORT : getMappedPort(KAFKA_PORT);
        }
    }
}
