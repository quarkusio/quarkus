package io.quarkus.kafka.streams.runtime;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

/**
 * Manages the lifecycle of a Kafka Streams pipeline. If there's a producer
 * method returning a KS {@link Topology}, then this topology will be configured
 * and started. Optionally, before starting the pipeline, this manager will wait
 * for a given set of topics to be created, as KS itself will fail without all
 * input topics being created upfront.
 */
@ApplicationScoped
public class KafkaStreamsTopologyManager {

    private static final Logger LOGGER = Logger.getLogger(KafkaStreamsTopologyManager.class.getName());

    private final ExecutorService executor;
    private volatile KafkaStreams streams;
    private volatile KafkaStreamsRuntimeConfig runtimeConfig;
    private volatile Instance<Topology> topology;
    private volatile Properties properties;
    private volatile Map<String, Object> adminClientConfig;

    KafkaStreamsTopologyManager() {
        executor = null;
    }

    @Inject
    public KafkaStreamsTopologyManager(Instance<Topology> topology) {
        // No producer for Topology -> nothing to do
        if (topology.isUnsatisfied()) {
            LOGGER.debug("No Topology producer; Kafka Streams will not be started");
            this.executor = null;
            return;
        }

        this.executor = Executors.newSingleThreadExecutor();
        this.topology = topology;
    }

    /**
     * Returns all properties to be passed to Kafka Streams.
     */
    private static Properties getStreamsProperties(Properties properties, String bootstrapServersConfig,
            KafkaStreamsRuntimeConfig runtimeConfig) {
        Properties streamsProperties = new Properties();

        // build-time options
        streamsProperties.putAll(properties);

        // add runtime options
        streamsProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServersConfig);
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, runtimeConfig.applicationId);

        if (runtimeConfig.applicationServer.isPresent()) {
            streamsProperties.put(StreamsConfig.APPLICATION_SERVER_CONFIG, runtimeConfig.applicationServer.get());
        }

        return streamsProperties;
    }

    private static String asString(List<InetSocketAddress> addresses) {
        return addresses.stream()
                .map(InetSocketAddress::toString)
                .collect(Collectors.joining(","));
    }

    void onStart(@Observes StartupEvent ev) {
        if (executor == null) {
            return;
        }

        String bootstrapServersConfig = asString(runtimeConfig.bootstrapServers);

        Properties streamsProperties = getStreamsProperties(properties, bootstrapServersConfig, runtimeConfig);

        streams = new KafkaStreams(topology.get(), streamsProperties);
        adminClientConfig = getAdminClientConfig(bootstrapServersConfig);

        executor.execute(() -> {
            try {
                waitForTopicsToBeCreated(runtimeConfig.getTrimmedTopics());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            LOGGER.debug("Starting Kafka Streams pipeline");
            streams.start();
        });
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (streams != null) {
            LOGGER.debug("Stopping Kafka Streams pipeline");
            streams.close();
        }
    }

    @Produces
    @Singleton
    public KafkaStreams getStreams() {
        return streams;
    }

    private void waitForTopicsToBeCreated(Collection<String> topicsToAwait)
            throws InterruptedException {
        try (AdminClient adminClient = AdminClient.create(adminClientConfig)) {
            while (true) {
                try {
                    ListTopicsResult topics = adminClient.listTopics();
                    Set<String> topicNames = topics.names().get(10, TimeUnit.SECONDS);

                    if (topicNames.containsAll(topicsToAwait)) {
                        LOGGER.debug("All expected topics created");
                        return;
                    } else {
                        Set<String> missing = new HashSet<>(topicsToAwait);
                        missing.removeAll(topicNames);
                        LOGGER.debug("Waiting for topic(s) to be created: " + missing);
                    }

                    Thread.sleep(1_000);
                } catch (ExecutionException | TimeoutException e) {
                    LOGGER.error("Failed to get topic names from broker", e);
                }
            }
        }
    }

    public Set<String> getMissingTopics(Collection<String> topicsToCheck)
            throws InterruptedException {
        HashSet<String> missing = new HashSet<>(topicsToCheck);

        try (AdminClient adminClient = AdminClient.create(adminClientConfig)) {
            ListTopicsResult topics = adminClient.listTopics();
            Set<String> topicNames = topics.names().get(10, TimeUnit.SECONDS);

            if (topicNames.containsAll(topicsToCheck)) {
                return Collections.EMPTY_SET;
            } else {
                missing.removeAll(topicNames);
            }
        } catch (ExecutionException | TimeoutException e) {
            LOGGER.error("Failed to get topic names from broker", e);
        }

        return missing;
    }

    private Map<String, Object> getAdminClientConfig(String bootstrapServersConfig) {
        Map<String, Object> adminClientConfig = new HashMap<>();
        adminClientConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServersConfig);
        // include other AdminClientConfig(s) that have been configured
        for (final String knownAdminClientConfig : AdminClientConfig.configNames()) {
            // give preference to admin.<propname> first
            if (properties.containsKey(StreamsConfig.ADMIN_CLIENT_PREFIX + knownAdminClientConfig)) {
                adminClientConfig.put(knownAdminClientConfig,
                        properties.get(StreamsConfig.ADMIN_CLIENT_PREFIX + knownAdminClientConfig));
            } else if (properties.containsKey(knownAdminClientConfig)) {
                adminClientConfig.put(knownAdminClientConfig, properties.get(knownAdminClientConfig));
            }
        }
        return adminClientConfig;
    }

    public void setRuntimeConfig(KafkaStreamsRuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void configure(Properties properties) {
        this.properties = properties;
    }
}
