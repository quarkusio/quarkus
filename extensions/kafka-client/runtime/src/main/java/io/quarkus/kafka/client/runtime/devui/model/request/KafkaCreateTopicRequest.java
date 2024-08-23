package io.quarkus.kafka.client.runtime.devui.model.request;

public class KafkaCreateTopicRequest {
    private String topicName;
    private Integer partitions;
    private Short replications;

    public KafkaCreateTopicRequest() {
    }

    public KafkaCreateTopicRequest(String topicName, Integer partitions, Short replications) {
        this.topicName = topicName;
        this.partitions = partitions;
        this.replications = replications;
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
}
