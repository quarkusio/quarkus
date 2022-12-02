package io.quarkus.kafka.client.runtime.ui.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties("action")
public class KafkaMessageCreateRequest {

    //TODO: add headers
    private String topic;
    private Integer partition;
    private String value;
    private String key;

    public KafkaMessageCreateRequest() {
    }

    public KafkaMessageCreateRequest(String topic, Integer partition, String value, String key) {
        this.topic = topic;
        this.partition = partition;
        this.value = value;
        this.key = key;
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
}
