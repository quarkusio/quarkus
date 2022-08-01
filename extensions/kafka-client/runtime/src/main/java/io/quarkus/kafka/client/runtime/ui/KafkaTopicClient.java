package io.quarkus.kafka.client.runtime.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.TopicPartitionInfo;

import io.smallrye.common.annotation.Identifier;

@Singleton
public class KafkaTopicClient {
    //TODO: inject me
    private AdminClient adminClient;

    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> config;

    @PostConstruct
    void init() {
        Map<String, Object> conf = new HashMap<>(config);
        conf.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
        adminClient = AdminClient.create(conf);
    }

    public List<Integer> partitions(String topicName) throws ExecutionException, InterruptedException {
        return adminClient.describeTopics(List.of(topicName))
                .allTopicNames()
                .get()
                .values().stream()
                .reduce((a, b) -> {
                    throw new IllegalStateException(
                            "Requested info about single topic, but got result of multiple: " + a + ", " + b);
                })
                .orElseThrow(() -> new IllegalStateException(
                        "Requested info about a topic, but nothing found. Topic name: " + topicName))
                .partitions().stream()
                .map(TopicPartitionInfo::partition)
                .collect(Collectors.toList());
    }
}
