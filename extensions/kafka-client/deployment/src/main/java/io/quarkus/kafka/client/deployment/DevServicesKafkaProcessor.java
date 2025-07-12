package io.quarkus.kafka.client.deployment;

import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.devservices.common.StartableContainer;
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

    @BuildStep
    public DevServicesResultBuildItem startKafkaDevService(
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem compose,
            LaunchModeBuildItem launchMode,
            KafkaBuildTimeConfig kafkaClientBuildTimeConfig,
            List<DevServicesSharedNetworkBuildItem> sharedNetwork,
            DevServicesConfig devServicesConfig) {
        // If the dev service is disabled, we return null to indicate that no dev service was started.
        KafkaDevServicesBuildTimeConfig config = kafkaClientBuildTimeConfig.devservices();
        if (devServiceDisabled(dockerStatusBuildItem, config)) {
            return null;
        }
        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig, sharedNetwork);

        return kafkaContainerLocator.locateContainer(config.serviceName(), config.shared(), launchMode.getLaunchMode())
                .or(() -> ComposeLocator.locateContainer(compose,
                        List.of(config.effectiveImageName(), "kafka", "strimzi", "redpanda"),
                        KAFKA_PORT, launchMode.getLaunchMode(), useSharedNetwork))
                .map(containerAddress -> {
                    createTopicPartitions(containerAddress.getUrl(), config);
                    return DevServicesResultBuildItem.discovered()
                            .feature(Feature.KAFKA_CLIENT)
                            .containerId(containerAddress.getId())
                            .config(Map.of(KAFKA_BOOTSTRAP_SERVERS, containerAddress.getUrl()))
                            .build();
                }).orElseGet(() -> DevServicesResultBuildItem.owned()
                        .feature(Feature.KAFKA_CLIENT)
                        .serviceName(config.serviceName())
                        .serviceConfig(config)
                        .startable(() -> createContainer(compose, config, useSharedNetwork))
                        .postStartHook(s -> logStartedAndCreateTopicPartitions(s.getConnectionInfo(), config))
                        .configProvider(Map.of(KAFKA_BOOTSTRAP_SERVERS, Startable::getConnectionInfo))
                        .build());
    }

    private Startable createContainer(DevServicesComposeProjectBuildItem composeProjectBuildItem,
            KafkaDevServicesBuildTimeConfig config,
            boolean useSharedNetwork) {
        Startable startable = switch (config.provider()) {
            case REDPANDA -> new RedpandaKafkaContainer(DockerImageName.parse(config.effectiveImageName())
                    .asCompatibleSubstituteFor("redpandadata/redpanda"),
                    config.port().orElse(0),
                    composeProjectBuildItem.getDefaultNetworkId(),
                    useSharedNetwork, config.redpanda())
                    .withEnv(config.containerEnv())
                    // Dev Service discovery works using a global dev service label applied in DevServicesCustomizerBuildItem
                    // for backwards compatibility we still add the custom label
                    .withLabel(DEV_SERVICE_LABEL, config.serviceName());
            case STRIMZI -> {
                StrimziKafkaContainer strimzi = new StrimziKafkaContainer(config.effectiveImageName())
                        .withBrokerId(1)
                        .withKraft()
                        .waitForRunning();
                String hostName = ConfigureUtil.configureNetwork(strimzi,
                        composeProjectBuildItem.getDefaultNetworkId(), useSharedNetwork, "kafka");
                if (useSharedNetwork) {
                    strimzi.withBootstrapServers(c -> String.format("PLAINTEXT://%s:%s", hostName, KAFKA_PORT));
                }
                if (config.port().isPresent() && config.port().get() != 0) {
                    strimzi.withPort(config.port().get());
                }
                strimzi.withEnv(config.containerEnv());
                strimzi.withLabel(DEV_SERVICE_LABEL, config.serviceName());
                yield new StartableContainer<>(strimzi, StrimziKafkaContainer::getBootstrapServers);
            }
            case KAFKA_NATIVE -> new KafkaNativeContainer(DockerImageName.parse(config.effectiveImageName()),
                    config.port().orElse(0),
                    composeProjectBuildItem.getDefaultNetworkId(),
                    useSharedNetwork)
                    .withEnv(config.containerEnv())
                    .withLabel(DEV_SERVICE_LABEL, config.serviceName());
        };
        return startable;
    }

    public void logStartedAndCreateTopicPartitions(String bootstrapServers, KafkaDevServicesBuildTimeConfig configuration) {
        logStarted(bootstrapServers);
        createTopicPartitions(bootstrapServers, configuration);
    }

    public void createTopicPartitions(String bootstrapServers, KafkaDevServicesBuildTimeConfig configuration) {
        Map<String, Integer> topicPartitions = configuration.topicPartitions();
        if (topicPartitions.isEmpty()) {
            return;
        }
        Map<String, Object> props = Map.ofEntries(
                Map.entry(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
                Map.entry(AdminClientConfig.CLIENT_ID_CONFIG, "kafka-devservices"));
        try (AdminClient adminClient = KafkaAdminClient.create(props)) {
            long adminClientTimeout = configuration.topicPartitionsTimeout().toMillis();
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

    private static void logStarted(String bootstrapServers) {
        log.infof(
                "Dev Services for Kafka started. Other Quarkus applications in dev mode will find the "
                        + "broker automatically. For Quarkus applications in production mode, you can connect to"
                        + " this by starting your application with -Dkafka.bootstrap.servers=%s",
                bootstrapServers);
    }

    private boolean devServiceDisabled(DockerStatusBuildItem dockerStatusBuildItem, KafkaDevServicesBuildTimeConfig config) {
        if (!config.enabled().orElse(true)) {
            // explicitly disabled
            log.debug("Not starting dev services for Kafka, as it has been disabled in the config.");
            return true;
        }

        // Check if kafka.bootstrap.servers is set
        if (ConfigUtils.isPropertyNonEmpty(KAFKA_BOOTSTRAP_SERVERS)) {
            log.debug("Not starting dev services for Kafka, the kafka.bootstrap.servers is configured.");
            return true;
        }

        // Verify that we have kafka channels without bootstrap.servers
        if (!hasKafkaChannelWithoutBootstrapServers()) {
            log.debug("Not starting dev services for Kafka, all the channels are configured.");
            return true;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn(
                    "Docker isn't working, please configure the Kafka bootstrap servers property (kafka.bootstrap.servers).");
            return true;
        }
        return false;
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

}
