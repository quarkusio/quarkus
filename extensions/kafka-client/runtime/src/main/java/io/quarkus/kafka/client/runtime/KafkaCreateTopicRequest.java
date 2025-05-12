package io.quarkus.kafka.client.runtime;

import java.util.Map;

public class KafkaCreateTopicRequest {
    private String topicName;
    private Integer partitions;
    private Short replications;
    private Map<String, String> configs;

    public KafkaCreateTopicRequest() {
    }

    public KafkaCreateTopicRequest(final String topicName, final Integer partitions, final Short replications,
            final Map<String, String> configs) {
        this.topicName = topicName;
        this.partitions = partitions;
        this.replications = replications;
        this.configs = configs;
    }

    public String getTopicName() {
        return topicName;
    }

    public Integer getPartitions() {
        return partitions;
    }

    public Short getReplications() {
        return replications;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

}
