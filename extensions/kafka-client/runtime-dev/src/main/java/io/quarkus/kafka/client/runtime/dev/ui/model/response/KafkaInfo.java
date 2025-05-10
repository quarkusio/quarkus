package io.quarkus.kafka.client.runtime.dev.ui.model.response;

import java.util.List;

public class KafkaInfo {
    private String broker;
    private KafkaClusterInfo clusterInfo;
    private List<KafkaTopic> topics;
    private List<KafkaConsumerGroup> consumerGroups;

    public KafkaInfo() {
    }

    public KafkaInfo(String broker, KafkaClusterInfo clusterInfo, List<KafkaTopic> topics,
            List<KafkaConsumerGroup> consumerGroups) {
        this.broker = broker;
        this.clusterInfo = clusterInfo;
        this.topics = topics;
        this.consumerGroups = consumerGroups;
    }

    public String getBroker() {
        return broker;
    }

    public List<KafkaTopic> getTopics() {
        return topics;
    }

    public KafkaClusterInfo getClusterInfo() {
        return clusterInfo;
    }

    public List<KafkaConsumerGroup> getConsumerGroups() {
        return consumerGroups;
    }
}
