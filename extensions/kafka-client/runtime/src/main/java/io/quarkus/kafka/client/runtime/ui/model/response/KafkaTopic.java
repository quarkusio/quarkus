package io.quarkus.kafka.client.runtime.ui.model.response;

public class KafkaTopic {
    private String name;
    private String topicId;
    private int partitionsCount;
    private boolean internal;

    public KafkaTopic() {
    }

    public KafkaTopic(String name, String topicId, int partitionsCount, boolean internal) {
        this.name = name;
        this.topicId = topicId;
        this.partitionsCount = partitionsCount;
        this.internal = internal;
    }

    public String getName() {
        return name;
    }

    public String getTopicId() {
        return topicId;
    }

    public int getPartitionsCount() {
        return partitionsCount;
    }

    public boolean isInternal() {
        return internal;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        sb.append(" : ").append(topicId);
        return sb.toString();
    }
}
