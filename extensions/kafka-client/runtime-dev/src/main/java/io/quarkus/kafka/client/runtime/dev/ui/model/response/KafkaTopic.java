package io.quarkus.kafka.client.runtime.dev.ui.model.response;

public class KafkaTopic {
    private String name;
    private String topicId;
    private int partitionsCount;
    private boolean internal;
    private long nmsg = 0;

    public KafkaTopic() {
    }

    public KafkaTopic(String name, String topicId, int partitionsCount, boolean internal, long nmsg) {
        this.name = name;
        this.topicId = topicId;
        this.partitionsCount = partitionsCount;
        this.internal = internal;
        this.nmsg = nmsg;
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

    public long getNmsg() {
        return nmsg;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        sb.append(" : ").append(topicId);
        return sb.toString();
    }
}
