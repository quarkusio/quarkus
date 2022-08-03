package io.quarkus.kafka.client.runtime.ui.model.response;

public class KafkaMessage {
    private final String topic;
    private final int partition;
    private final long offset;
    private final long timestamp;
    private final String key;
    private final String value;

    public KafkaMessage(String topic, int partition, long offset, long timestamp, String key, String value) {
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.timestamp = timestamp;
        this.key = key;
        this.value = value;
    }

    public String getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }

    public long getOffset() {
        return offset;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
