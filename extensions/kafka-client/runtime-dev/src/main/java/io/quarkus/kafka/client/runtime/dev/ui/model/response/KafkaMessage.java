package io.quarkus.kafka.client.runtime.dev.ui.model.response;

import java.util.Map;

public class KafkaMessage {
    private final String topic;
    private final int partition;
    private final long offset;
    private final long timestamp;
    private final String key;
    private final String value;
    private final Map<String, String> headers;

    public KafkaMessage(String topic, int partition, long offset, long timestamp, String key, String value,
            Map<String, String> headers) {
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.timestamp = timestamp;
        this.key = key;
        this.value = value;
        this.headers = headers;
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

    public Map<String, String> getHeaders() {
        return headers;
    }
}
