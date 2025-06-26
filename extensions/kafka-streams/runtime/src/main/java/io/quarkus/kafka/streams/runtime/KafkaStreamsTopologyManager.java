package io.quarkus.kafka.streams.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyDescription;
import org.jboss.logging.Logger;

public class KafkaStreamsTopologyManager {

    private static final Logger LOGGER = Logger.getLogger(KafkaStreamsTopologyManager.class.getName());

    private final Admin adminClient;
    private final List<String> sourceTopics;
    private final List<Pattern> sourcePatterns;
    private final Duration topicsTimeout;

    private volatile boolean closed = false;

    public KafkaStreamsTopologyManager(Admin adminClient, Topology topology, KafkaStreamsRuntimeConfig runtimeConfig) {
        this.adminClient = adminClient;
        this.topicsTimeout = runtimeConfig.topicsTimeout();
        if (isTopicsCheckEnabled()) {
            if (runtimeConfig.topics().isEmpty() && runtimeConfig.topicPatterns().isEmpty()) {
                Set<String> topics = new HashSet<>();
                Set<Pattern> patterns = new HashSet<>();
                extractSources(topology, topics, patterns);
                this.sourceTopics = new ArrayList<>(topics);
                this.sourcePatterns = new ArrayList<>(patterns);
                LOGGER.infof("Kafka Streams will wait for topics: %s and topics matching patterns: %s to be created",
                        sourceTopics, sourcePatterns);
            } else {
                this.sourceTopics = runtimeConfig.topics().orElse(Collections.emptyList());
                this.sourcePatterns = runtimeConfig.topicPatterns().orElse(Collections.emptyList()).stream()
                        .map(Pattern::compile)
                        .toList();
            }
            if (sourceTopics.isEmpty() && sourcePatterns.isEmpty()) {
                throw new IllegalArgumentException(
                        "No topics or topic patterns specified; cannot wait for topics to be created, " +
                                "in order to disable topics creation check set `quarkus.kafka-streams.topics-check-timeout=0`");
            }
        } else {
            LOGGER.infof("Kafka Streams will not wait for topics to be created");
            this.sourceTopics = Collections.emptyList();
            this.sourcePatterns = Collections.emptyList();
        }
    }

    public boolean isTopicsCheckEnabled() {
        return topicsTimeout.compareTo(Duration.ZERO) > 0;
    }

    void close() {
        this.closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    // visible for testing
    public static void extractSources(Topology topo, Set<String> topics, Set<Pattern> patterns) {
        Set<String> sinkTopics = new HashSet<>();
        TopologyDescription topologyDescription = topo.describe();
        for (TopologyDescription.GlobalStore globalStore : topologyDescription.globalStores()) {
            TopologyDescription.Source source = globalStore.source();
            if (source.topicPattern() != null) {
                patterns.add(source.topicPattern());
            }
            if (source.topicSet() != null) {
                topics.addAll(source.topicSet());
            }
        }
        for (TopologyDescription.Subtopology subtopology : topologyDescription.subtopologies()) {
            for (TopologyDescription.Node node : subtopology.nodes()) {
                if (node instanceof TopologyDescription.Sink sink) {
                    if (sink.topic() != null) {
                        sinkTopics.add(sink.topic());
                    }
                } else if (node instanceof TopologyDescription.Source source) {
                    if (source.topicPattern() != null) {
                        patterns.add(source.topicPattern());
                    }
                    if (source.topicSet() != null) {
                        topics.addAll(source.topicSet());
                    }
                }
            }
        }
        // remove topics used both in sinks ans sources
        topics.removeAll(sinkTopics);
    }

    public List<String> getSourceTopics() {
        return sourceTopics;
    }

    public List<Pattern> getSourcePatterns() {
        return sourcePatterns;
    }

    public Set<String> getMissingTopics() throws InterruptedException {
        if (!isTopicsCheckEnabled()) {
            return Collections.emptySet();
        }

        Set<String> missing = new LinkedHashSet<>(sourceTopics);

        try {
            ListTopicsResult topics = adminClient.listTopics();
            Set<String> existingTopics = topics.names().get(topicsTimeout.toMillis(), TimeUnit.MILLISECONDS);

            missing.removeAll(existingTopics);
            missing.addAll(sourcePatterns.stream()
                    .filter(p -> existingTopics.stream().noneMatch(p.asPredicate()))
                    .map(Pattern::pattern)
                    .toList());
        } catch (ExecutionException | TimeoutException e) {
            LOGGER.error("Failed to get topic names from broker", e);
        }

        return missing;
    }

    public void waitForTopicsToBeCreated() throws InterruptedException {
        if (!isTopicsCheckEnabled()) {
            return;
        }
        Set<String> lastMissingTopics = null;
        while (!closed) {
            try {
                ListTopicsResult topics = adminClient.listTopics();
                Set<String> existingTopics = topics.names().get(topicsTimeout.toMillis(), TimeUnit.MILLISECONDS);

                if (existingTopics.containsAll(sourceTopics)
                        && sourcePatterns.stream().allMatch(p -> existingTopics.stream().anyMatch(p.asPredicate()))) {
                    LOGGER.debugf("All expected topics %s and topics matching patterns %s ", sourceTopics, sourcePatterns);
                    return;
                } else {
                    Set<String> missingTopics = new HashSet<>(sourceTopics);
                    missingTopics.removeAll(existingTopics);

                    // Do not spam warnings - topics may take time to be created by an operator like Strimzi
                    if (missingTopics.equals(lastMissingTopics)) {
                        LOGGER.debug("Waiting for topic(s) to be created: " + missingTopics);
                    } else {
                        LOGGER.warn("Waiting for topic(s) to be created: " + missingTopics);
                        lastMissingTopics = missingTopics;
                    }
                }
            } catch (ExecutionException | TimeoutException e) {
                LOGGER.error("Failed to get topic names from broker", e);
            } finally {
                Thread.sleep(1_000);
            }
        }
    }
}
