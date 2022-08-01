package io.quarkus.kafka.client.runtime.ui.model.response;

import java.util.List;

public class KafkaInfo {
    private String broker;
    private KafkaClusterInfo clusterInfo;
    private List<KafkaTopic> topics;

    public KafkaInfo() {
    }

    public KafkaInfo(String broker, KafkaClusterInfo clusterInfo, List<KafkaTopic> topics) {
        this.broker = broker;
        this.clusterInfo = clusterInfo;
        this.topics = topics;
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

}
