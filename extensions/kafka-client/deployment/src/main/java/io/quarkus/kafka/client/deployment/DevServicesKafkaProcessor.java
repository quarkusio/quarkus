package io.quarkus.kafka.client.deployment;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.time.Duration;
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
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.common.Labels;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.strimzi.test.container.StrimziKafkaContainer;

/**
 * Starts a Kafka broker as dev service if needed.
 */
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class DevServicesKafkaProcessor {

    private static final Logger log = Logger.getLogger(DevServicesKafkaProcessor.class);
    private static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";

    /**
     * Label to add to shared Dev Service for Kafka running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-kafka";
    static final int KAFKA_PORT = 9092;

    private static final ContainerLocator kafkaContainerLocator = locateContainerWithLabels(KAFKA_PORT, DEV_SERVICE_LABEL);

    static volatile RunningDevService devService;
    static volatile KafkaDevServiceCfg cfg;
    static volatile boolean first = true;

    @BuildStep
    public DevServicesResultBuildItem startKafkaDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            LaunchModeBuildItem launchMode,
            KafkaBuildTimeConfig kafkaClientBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem, DevServicesConfig devServicesConfig) {

        KafkaDevServiceCfg configuration = getConfiguration(kafkaClientBuildTimeConfig);

        if (devService != null && devService.isOwner()) {
            boolean shouldShutdownTheBroker = !configuration.equals(cfg);
            if (!shouldShutdownTheBroker) {
                return devService.toBuildItem();
            }
            shutdownBroker();
            cfg = null;
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Kafka Dev Services Starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem);
        try {
            devService = startKafka(dockerStatusBuildItem, composeProjectBuildItem, configuration, launchMode,
                    !devServicesSharedNetworkBuildItem.isEmpty(),
                    devServicesConfig.timeout());
            if (devService == null) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (devService == null) {
            return null;
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (devService != null) {
                    shutdownBroker();
                }
                first = true;
                devService = null;
                cfg = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        cfg = configuration;

        if (devService.isOwner()) {
            log.infof(
                    "Dev Services for Kafka started. Other Quarkus applications in dev mode will find the "
                            + "broker automatically. For Quarkus applications in production mode, you can connect to"
                            + " this by starting your application with -Dkafka.bootstrap.servers=%s",
                    getKafkaBootstrapServers());
        }
        createTopicPartitions(getKafkaBootstrapServers(), configuration);
        return devService.toBuildItem();
    }

    public static String getKafkaBootstrapServers() {
        return devService.getConfig().get(KAFKA_BOOTSTRAP_SERVERS);
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
            Map<String, TopicDescription> partitions = adminClient.describeTopics(currentTopics).allTopicNames()
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
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop the Kafka broker", e);
            } finally {
                devService = null;
            }
        }
    }

    private RunningDevService startKafka(DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            KafkaDevServiceCfg config,
            LaunchModeBuildItem launchMode, boolean useSharedNetwork, Optional<Duration> timeout) {
        if (!config.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting dev services for Kafka, as it has been disabled in the config.");
            return null;
        }

        // Check if kafka.bootstrap.servers is set
        if (ConfigUtils.isPropertyNonEmpty(KAFKA_BOOTSTRAP_SERVERS)) {
            log.debug("Not starting dev services for Kafka, the kafka.bootstrap.servers is configured.");
            return null;
        }

        // Verify that we have kafka channels without bootstrap.servers
        if (!hasKafkaChannelWithoutBootstrapServers()) {
            log.debug("Not starting dev services for Kafka, all the channels are configured.");
            return null;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn(
                    "Docker isn't working, please configure the Kafka bootstrap servers property (kafka.bootstrap.servers).");
            return null;
        }

        // Starting the broker
        final Supplier<RunningDevService> defaultKafkaBrokerSupplier = () -> {
            switch (config.provider) {
                case REDPANDA:
                    RedpandaKafkaContainer redpanda = new RedpandaKafkaContainer(
                            DockerImageName.parse(config.imageName).asCompatibleSubstituteFor("redpandadata/redpanda"),
                            config.fixedExposedPort,
                            launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName : null,
                            composeProjectBuildItem.getDefaultNetworkId(),
                            useSharedNetwork, config.redpanda);
                    timeout.ifPresent(redpanda::withStartupTimeout);
                    redpanda.withEnv(config.containerEnv);
                    redpanda.start();

                    return new RunningDevService(Feature.KAFKA_CLIENT.getName(),
                            redpanda.getContainerId(),
                            redpanda::close,
                            KAFKA_BOOTSTRAP_SERVERS, redpanda.getBootstrapServers());
                case STRIMZI:
                    StrimziKafkaContainer strimzi = new StrimziKafkaContainer(config.imageName)
                            .withBrokerId(1)
                            .withKraft()
                            .waitForRunning();
                    String hostName = ConfigureUtil.configureNetwork(strimzi,
                            composeProjectBuildItem.getDefaultNetworkId(), useSharedNetwork, "kafka");
                    if (useSharedNetwork) {
                        strimzi.withBootstrapServers(c -> String.format("PLAINTEXT://%s:%s", hostName, KAFKA_PORT));
                    }
                    if (config.serviceName != null) {
                        strimzi.withLabel(DevServicesKafkaProcessor.DEV_SERVICE_LABEL, config.serviceName);
                        strimzi.withLabel(Labels.QUARKUS_DEV_SERVICE, config.serviceName);
                    }
                    if (config.fixedExposedPort != 0) {
                        strimzi.withPort(config.fixedExposedPort);
                    }
                    timeout.ifPresent(strimzi::withStartupTimeout);
                    strimzi.withEnv(config.containerEnv);

                    strimzi.start();
                    return new RunningDevService(Feature.KAFKA_CLIENT.getName(),
                            strimzi.getContainerId(),
                            strimzi::close,
                            KAFKA_BOOTSTRAP_SERVERS, strimzi.getBootstrapServers());
                case KAFKA_NATIVE:
                    KafkaNativeContainer kafkaNative = new KafkaNativeContainer(DockerImageName.parse(config.imageName),
                            config.fixedExposedPort,
                            launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName : null,
                            composeProjectBuildItem.getDefaultNetworkId(),
                            useSharedNetwork);
                    timeout.ifPresent(kafkaNative::withStartupTimeout);
                    kafkaNative.withEnv(config.containerEnv);
                    kafkaNative.start();

                    return new RunningDevService(Feature.KAFKA_CLIENT.getName(),
                            kafkaNative.getContainerId(),
                            kafkaNative::close,
                            KAFKA_BOOTSTRAP_SERVERS, kafkaNative.getBootstrapServers());
            }
            return null;
        };

        return kafkaContainerLocator.locateContainer(config.serviceName, config.shared, launchMode.getLaunchMode())
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(config.imageName, "kafka", "strimzi", "redpanda"),
                        KAFKA_PORT, launchMode.getLaunchMode(), useSharedNetwork))
                .map(containerAddress -> new RunningDevService(Feature.KAFKA_CLIENT.getName(),
                        containerAddress.getId(),
                        null,
                        KAFKA_BOOTSTRAP_SERVERS, containerAddress.getUrl()))
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
                isConfigured = ConfigUtils.isPropertyNonEmpty(name.replace(".connector", ".bootstrap.servers"));
            }
            if (!isConfigured) {
                return true;
            }
        }
        return false;
    }

    private KafkaDevServiceCfg getConfiguration(KafkaBuildTimeConfig cfg) {
        KafkaDevServicesBuildTimeConfig devServicesConfig = cfg.devservices();
        return new KafkaDevServiceCfg(devServicesConfig);
    }

    private static final class KafkaDevServiceCfg {
        private final boolean devServicesEnabled;
        private final String imageName;
        private final Integer fixedExposedPort;
        private final boolean shared;
        private final String serviceName;
        private final Map<String, Integer> topicPartitions;
        private final Duration topicPartitionsTimeout;
        private final Map<String, String> containerEnv;

        private final KafkaDevServicesBuildTimeConfig.Provider provider;

        private final RedpandaBuildTimeConfig redpanda;

        public KafkaDevServiceCfg(KafkaDevServicesBuildTimeConfig config) {
            this.devServicesEnabled = config.enabled().orElse(true);
            this.provider = config.provider();
            this.imageName = config.imageName().orElseGet(provider::getDefaultImageName);
            this.fixedExposedPort = config.port().orElse(0);
            this.shared = config.shared();
            this.serviceName = config.serviceName();
            this.topicPartitions = config.topicPartitions();
            this.topicPartitionsTimeout = config.topicPartitionsTimeout();
            this.containerEnv = config.containerEnv();

            this.redpanda = config.redpanda();
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
            return devServicesEnabled == that.devServicesEnabled
                    && Objects.equals(provider, that.provider)
                    && Objects.equals(imageName, that.imageName)
                    && Objects.equals(fixedExposedPort, that.fixedExposedPort)
                    && Objects.equals(containerEnv, that.containerEnv);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, provider, imageName, fixedExposedPort, containerEnv);
        }
    }

}
