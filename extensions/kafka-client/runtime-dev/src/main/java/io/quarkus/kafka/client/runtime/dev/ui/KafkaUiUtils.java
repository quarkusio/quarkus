package io.quarkus.kafka.client.runtime.dev.ui;

import static io.quarkus.kafka.client.runtime.dev.ui.util.ConsumerFactory.createConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.netty.buffer.ByteBufInputStream;
import io.quarkus.kafka.client.runtime.KafkaAdminClient;
import io.quarkus.kafka.client.runtime.dev.ui.model.Order;
import io.quarkus.kafka.client.runtime.dev.ui.model.request.KafkaMessageCreateRequest;
import io.quarkus.kafka.client.runtime.dev.ui.model.request.KafkaMessagesRequest;
import io.quarkus.kafka.client.runtime.dev.ui.model.request.KafkaOffsetRequest;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaAclEntry;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaAclInfo;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaClusterInfo;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaConsumerGroup;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaConsumerGroupMember;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaConsumerGroupMemberPartitionAssignment;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaInfo;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaMessagePage;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaNode;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaTopic;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

@Singleton
public class KafkaUiUtils {

    private final KafkaAdminClient kafkaAdminClient;

    private final KafkaTopicClient kafkaTopicClient;
    private final ObjectMapper objectMapper;

    private final Map<String, Object> config;

    public KafkaUiUtils(KafkaAdminClient kafkaAdminClient, KafkaTopicClient kafkaTopicClient,
            @Identifier("default-kafka-broker") Map<String, Object> config) {
        this.kafkaAdminClient = kafkaAdminClient;
        this.kafkaTopicClient = kafkaTopicClient;
        this.config = config;
        this.objectMapper = JsonMapper.builder()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public KafkaInfo getKafkaInfo() throws ExecutionException, InterruptedException {
        var clusterInfo = getClusterInfo();
        var broker = clusterInfo.getController().asFullNodeName();
        var topics = getTopics();
        var consumerGroups = getConsumerGroups();
        return new KafkaInfo(broker, clusterInfo, topics, consumerGroups);
    }

    public KafkaClusterInfo getClusterInfo() throws ExecutionException, InterruptedException {
        return clusterInfo(kafkaAdminClient.getCluster());
    }

    private KafkaNode kafkaNode(Node node) {
        return new KafkaNode(node.host(), node.port(), node.idString());
    }

    private KafkaClusterInfo clusterInfo(DescribeClusterResult dcr) throws InterruptedException, ExecutionException {
        var controller = kafkaNode(dcr.controller().get());
        var nodes = new ArrayList<KafkaNode>();
        for (var node : dcr.nodes().get()) {
            nodes.add(kafkaNode(node));
        }
        var aclOperations = dcr.authorizedOperations().get();

        var aclOperationsStr = new StringBuilder();
        if (aclOperations != null) {
            for (var operation : dcr.authorizedOperations().get()) {
                if (aclOperationsStr.length() == 0) {
                    aclOperationsStr.append(", ");
                }
                aclOperationsStr.append(operation.name());
            }
        } else {
            aclOperationsStr = new StringBuilder("NONE");
        }

        return new KafkaClusterInfo(
                dcr.clusterId().get(),
                controller,
                nodes,
                aclOperationsStr.toString());
    }

    public List<KafkaTopic> getTopics() throws InterruptedException, ExecutionException {
        var res = new ArrayList<KafkaTopic>();
        for (TopicListing tl : kafkaAdminClient.getTopics()) {
            res.add(kafkaTopic(tl));
        }
        return res;
    }

    private KafkaTopic kafkaTopic(TopicListing tl) throws ExecutionException, InterruptedException {
        var partitions = partitions(tl.name());
        return new KafkaTopic(
                tl.name(),
                tl.topicId().toString(),
                partitions.size(),
                tl.isInternal(),
                getTopicMessageCount(tl.name(), partitions));
    }

    public long getTopicMessageCount(String topicName, Collection<Integer> partitions)
            throws ExecutionException, InterruptedException {
        var maxPartitionOffsetMap = kafkaTopicClient.getPagePartitionOffset(topicName, partitions, Order.NEW_FIRST);
        return maxPartitionOffsetMap.values().stream()
                .reduce(Long::sum)
                .orElse(0L);
    }

    public Collection<Integer> partitions(String topicName) throws ExecutionException, InterruptedException {
        return kafkaTopicClient.partitions(topicName);
    }

    public KafkaMessagePage getMessages(KafkaMessagesRequest request) throws ExecutionException, InterruptedException {
        return kafkaTopicClient.getTopicMessages(request.getTopicName(), request.getOrder(), request.getPartitionOffset(),
                request.getPageSize());
    }

    public void createMessage(KafkaMessageCreateRequest request) {
        kafkaTopicClient.createMessage(request);
    }

    public List<KafkaConsumerGroup> getConsumerGroups() throws InterruptedException, ExecutionException {
        List<KafkaConsumerGroup> res = new ArrayList<>();
        for (ConsumerGroupDescription cgd : kafkaAdminClient.getConsumerGroups()) {

            var metadata = kafkaAdminClient.listConsumerGroupOffsets(cgd.groupId())
                    .partitionsToOffsetAndMetadata().get();
            var members = cgd.members().stream()
                    .map(member -> new KafkaConsumerGroupMember(
                            member.consumerId(),
                            member.clientId(),
                            member.host(),
                            getPartitionAssignments(metadata, member)))
                    .collect(Collectors.toSet());

            res.add(new KafkaConsumerGroup(
                    cgd.groupId(),
                    cgd.state().name(),
                    cgd.coordinator().host(),
                    cgd.coordinator().id(),
                    cgd.partitionAssignor(),
                    getTotalLag(members),
                    members));
        }
        return res;
    }

    private long getTotalLag(Set<KafkaConsumerGroupMember> members) {
        return members.stream()
                .map(KafkaConsumerGroupMember::getPartitions)
                .flatMap(Collection::stream)
                .map(KafkaConsumerGroupMemberPartitionAssignment::getLag)
                .reduce(Long::sum)
                .orElse(0L);
    }

    private Set<KafkaConsumerGroupMemberPartitionAssignment> getPartitionAssignments(
            Map<TopicPartition, OffsetAndMetadata> topicOffsetMap, MemberDescription member) {
        var topicPartitions = member.assignment().topicPartitions();
        try (var consumer = createConsumer(topicPartitions, config)) {
            var endOffsets = consumer.endOffsets(topicPartitions);

            return topicPartitions.stream()
                    .map(tp -> {
                        var topicOffset = Optional.ofNullable(topicOffsetMap.get(tp))
                                .map(OffsetAndMetadata::offset)
                                .orElse(0L);
                        return new KafkaConsumerGroupMemberPartitionAssignment(tp.partition(), tp.topic(),
                                getLag(topicOffset, endOffsets.get(tp)));
                    })
                    .collect(Collectors.toSet());
        }
    }

    private long getLag(long topicOffset, long endOffset) {
        return endOffset - topicOffset;
    }

    public Map<Integer, Long> getOffset(KafkaOffsetRequest request) throws ExecutionException, InterruptedException {
        return kafkaTopicClient.getPagePartitionOffset(request.getTopicName(), request.getRequestedPartitions(),
                request.getOrder());
    }

    public KafkaAclInfo getAclInfo() throws InterruptedException, ExecutionException {
        var clusterInfo = clusterInfo(kafkaAdminClient.getCluster());
        var entries = new ArrayList<KafkaAclEntry>();
        //TODO: fix it after proper error message impl
        try {
            var acls = kafkaAdminClient.getAclInfo();
            for (var acl : acls) {
                var entry = new KafkaAclEntry(
                        acl.entry().operation().name(),
                        acl.entry().principal(),
                        acl.entry().permissionType().name(),
                        acl.pattern().toString());
                entries.add(entry);
            }
        } catch (Exception e) {
            // this mostly means that ALC controller is absent
        }
        return new KafkaAclInfo(
                clusterInfo.getId(),
                clusterInfo.getController().asFullNodeName(),
                clusterInfo.getAclOperations(),
                entries);
    }

    public String toJson(Object o) {
        String res;
        try {
            res = objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException ex) {
            res = "";
        }
        return res;
    }

    public JsonObject fromJson(Buffer buffer) {
        return new JsonObject(fromJson(buffer, Map.class));
    }

    public <T> T fromJson(Buffer buffer, Class<T> type) {
        try {
            JsonParser parser = objectMapper.createParser((InputStream) new ByteBufInputStream(buffer.getByteBuf()));
            return objectMapper.readValue(parser, type);
        } catch (IOException e) {
            return null;
        }
    }

}
