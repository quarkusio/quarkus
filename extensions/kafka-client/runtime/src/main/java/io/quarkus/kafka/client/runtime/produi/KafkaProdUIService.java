package io.quarkus.kafka.client.runtime.produi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.utils.Bytes;
import org.jboss.logging.Logger;

import io.quarkus.kafka.client.runtime.KafkaAdminClient;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.Identifier;

/**
 * Read-only view of the Kafka cluster, shared by Dev UI and Prod UI. It exposes
 * only cluster/broker metadata, the topic list and consumer-group status
 * (including partition lag) - no topic creation/deletion, no message browsing or
 * production, and no secrets. It is built entirely on the always-present
 * {@link KafkaAdminClient} (the richer Dev UI helpers live in the dev-only
 * module) and returns plain records so no JSON library is needed on the runtime
 * classpath.
 */
@ApplicationScoped
public class KafkaProdUIService {

    private static final Logger LOG = Logger.getLogger(KafkaProdUIService.class);

    @Inject
    KafkaAdminClient kafkaAdminClient;

    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> config;

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only overview of the Kafka cluster: brokers, topics and consumer groups")
    public ClusterOverview getOverview() throws Exception {
        Node controller = kafkaAdminClient.getCluster().controller().get();
        String clusterId = kafkaAdminClient.getCluster().clusterId().get();

        List<NodeInfo> nodes = new ArrayList<>();
        for (Node node : kafkaAdminClient.getCluster().nodes().get()) {
            nodes.add(new NodeInfo(node.idString(), node.host(), node.port()));
        }

        return new ClusterOverview(clusterId, asFullName(controller), nodes, getTopics(), getConsumerGroups());
    }

    private List<TopicInfo> getTopics() throws Exception {
        Collection<TopicListing> listings = kafkaAdminClient.getTopics();
        List<String> names = new ArrayList<>();
        for (TopicListing tl : listings) {
            names.add(tl.name());
        }
        Map<String, TopicDescription> descriptions = kafkaAdminClient.describeTopics(names);

        List<TopicInfo> topics = new ArrayList<>();
        for (TopicListing tl : listings) {
            TopicDescription desc = descriptions.get(tl.name());
            int partitions = desc != null ? desc.partitions().size() : 0;
            topics.add(new TopicInfo(tl.name(), tl.topicId().toString(), partitions, tl.isInternal()));
        }
        return topics;
    }

    private List<GroupInfo> getConsumerGroups() throws Exception {
        List<GroupInfo> groups = new ArrayList<>();
        for (ConsumerGroupDescription cgd : kafkaAdminClient.getConsumerGroups()) {
            groups.add(new GroupInfo(
                    cgd.groupId(),
                    cgd.state().name(),
                    asFullName(cgd.coordinator()),
                    cgd.partitionAssignor(),
                    cgd.members().size(),
                    getTotalLag(cgd.groupId())));
        }
        return groups;
    }

    /**
     * Total lag of a consumer group = sum over its committed partitions of
     * (log-end-offset - committed-offset). Best-effort: any failure (e.g. no
     * committed offsets, broker unavailable) is logged and reported as -1.
     */
    private long getTotalLag(String groupId) {
        try {
            Map<TopicPartition, OffsetAndMetadata> committed = kafkaAdminClient.listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata().get();
            if (committed.isEmpty()) {
                return 0L;
            }
            try (Consumer<Bytes, Bytes> consumer = createConsumer(committed.keySet())) {
                Map<TopicPartition, Long> endOffsets = consumer.endOffsets(committed.keySet());
                long lag = 0L;
                for (TopicPartition tp : committed.keySet()) {
                    OffsetAndMetadata offset = committed.get(tp);
                    Long end = endOffsets.get(tp);
                    if (offset != null && end != null) {
                        lag += Math.max(0L, end - offset.offset());
                    }
                }
                return lag;
            }
        } catch (Exception e) {
            LOG.debugf(e, "Unable to compute lag for consumer group '%s'", groupId);
            return -1L;
        }
    }

    private Consumer<Bytes, Bytes> createConsumer(Collection<TopicPartition> partitions) {
        Map<String, Object> consumerConfig = new HashMap<>(config);
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, BytesDeserializer.class);
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, BytesDeserializer.class);
        consumerConfig.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafka-produi-" + UUID.randomUUID());
        consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        KafkaConsumer<Bytes, Bytes> consumer = new KafkaConsumer<>(consumerConfig);
        consumer.assign(partitions);
        return consumer;
    }

    private static String asFullName(Node node) {
        if (node == null) {
            return "";
        }
        return node.host() + ":" + node.port() + " (" + node.idString() + ")";
    }

    public record NodeInfo(String id, String host, int port) {
    }

    public record TopicInfo(String name, String id, int partitions, boolean internal) {
    }

    public record GroupInfo(String groupId, String state, String coordinator, String protocol, int members, long lag) {
    }

    public record ClusterOverview(String clusterId, String controller, List<NodeInfo> nodes, List<TopicInfo> topics,
            List<GroupInfo> groups) {
    }
}
