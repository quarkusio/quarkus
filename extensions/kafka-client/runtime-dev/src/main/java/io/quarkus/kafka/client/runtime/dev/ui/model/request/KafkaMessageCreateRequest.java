package io.quarkus.kafka.client.runtime.dev.ui.model.request;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties("action")
public class KafkaMessageCreateRequest {

    private String topic;
    private Integer partition;
    private String value;
    private String key;
    private Map<String, String> headers;

    public KafkaMessageCreateRequest() {
    }

    public KafkaMessageCreateRequest(String topic, Integer partition, String value, String key, Map<String, String> headers) {
        this.topic = topic;
        this.partition = partition;
        this.value = value;
        this.key = key;
        this.headers = headers;
    }

    public String getTopic() {
        return topic;
    }

    public Integer getPartition() {
        return partition;
    }

    public String getValue() {
        return value;
    }

    public String getKey() {
        return key;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
