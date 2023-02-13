package io.quarkus.kafka.client.runtime;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.resource.ResourcePatternFilter;

import io.quarkus.kafka.client.runtime.ui.model.request.KafkaCreateTopicRequest;
import io.smallrye.common.annotation.Identifier;

@ApplicationScoped
public class KafkaAdminClient {
    private static final int DEFAULT_ADMIN_CLIENT_TIMEOUT = 5000;

    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> config;

    private AdminClient client;

    @PostConstruct
    void init() {
        Map<String, Object> conf = new HashMap<>();
        conf.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, DEFAULT_ADMIN_CLIENT_TIMEOUT);
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (AdminClientConfig.configNames().contains(entry.getKey())) {
                conf.put(entry.getKey(), entry.getValue().toString());
            }
        }
        client = AdminClient.create(conf);
    }

    @PreDestroy
    void stop() {
        client.close();
    }

    public DescribeClusterResult getCluster() {
        return client.describeCluster();
    }

    public Collection<TopicListing> getTopics() throws InterruptedException, ExecutionException {
        return client.listTopics().listings().get();
    }

    public Collection<ConsumerGroupDescription> getConsumerGroups() throws InterruptedException, ExecutionException {
        var consumerGroupIds = client.listConsumerGroups().all().get().stream()
                .map(ConsumerGroupListing::groupId)
                .collect(Collectors.toList());
        return client.describeConsumerGroups(consumerGroupIds).all().get()
                .values();
    }

    public boolean deleteTopic(String name) {
        Collection<String> topics = new ArrayList<>();
        topics.add(name);
        DeleteTopicsResult dtr = client.deleteTopics(topics);
        return dtr.topicNameValues() != null;
    }

    public boolean createTopic(KafkaCreateTopicRequest kafkaCreateTopicRq) {
        var partitions = Optional.ofNullable(kafkaCreateTopicRq.getPartitions()).orElse(1);
        var replications = Optional.ofNullable(kafkaCreateTopicRq.getReplications()).orElse((short) 1);
        var newTopic = new NewTopic(kafkaCreateTopicRq.getTopicName(), partitions, replications);

        CreateTopicsResult ctr = client.createTopics(List.of(newTopic));
        return ctr.values() != null;
    }

    public ListConsumerGroupOffsetsResult listConsumerGroupOffsets(String groupId) {
        return client.listConsumerGroupOffsets(groupId);
    }

    public Collection<AclBinding> getAclInfo() throws InterruptedException, ExecutionException {
        AclBindingFilter filter = new AclBindingFilter(ResourcePatternFilter.ANY, AccessControlEntryFilter.ANY);
        var options = new DescribeAclsOptions().timeoutMs(1_000);
        return client.describeAcls(filter, options).values().get();
    }

    public Map<String, TopicDescription> describeTopics(Collection<String> topicNames)
            throws InterruptedException, ExecutionException {
        return client.describeTopics(topicNames)
                .allTopicNames()
                .get();
    }
}
